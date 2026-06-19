import pika

class RabbitMq:
    def __init__(self, config):
        self.config = config
        self.credentials = pika.PlainCredentials(config['username'], config['password'])

        # Parametrii de conexiune corect transformați din tuplu în obiect normal
        self.parameters = pika.ConnectionParameters(
            host=config['host'],
            port=config['port'],
            credentials=self.credentials
        )

        self.current_message = None

    # Aceasta este funcția callback de care se plângea eroarea!
    def on_received_message(self, blocking_channel, deliver, properties, message):
        result = message.decode('utf-8')
        blocking_channel.confirm_delivery()
        try:
            print(result)
            self.current_message = result
        except Exception as e:
            print(e)
            print("wrong data format")
        finally:
            # Oprește ascultarea pentru a returna mesajul curent în apelul receive_message()
            blocking_channel.stop_consuming()

    def receive_message(self):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:

                # Declarăm automat infrastructura ca să evităm erorile 404 (NOT_FOUND)
                channel.exchange_declare(exchange=self.config['exchange'], exchange_type='direct', durable=True)
                channel.queue_declare(queue=self.config['queue'], durable=True)
                channel.queue_bind(exchange=self.config['exchange'], queue=self.config['queue'], routing_key=self.config['routing_key'])

                channel.basic_consume(self.config['queue'],
                                      self.on_received_message,
                                      auto_ack=True)
                try:
                    channel.start_consuming()
                except Exception:
                    print("Connection closed by broker.")
                except KeyboardInterrupt:
                    print("Application closed.")

        return self.current_message

    def send_message(self, message):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:

                channel.exchange_declare(exchange=self.config['exchange'], exchange_type='direct', durable=True)

                channel.basic_publish(
                    exchange=self.config['exchange'],
                    routing_key=self.config['routing_key'],
                    body=message)