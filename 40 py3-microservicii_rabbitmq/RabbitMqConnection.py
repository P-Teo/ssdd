import pika
from retry import retry


class RabbitMqInterface:
    def __init__(self):
        self.config = {
            'host': 'localhost',      # Folosim 'localhost' pe Windows pentru stabilitate
            'port': 5679,             # Portul mapat în Docker
            'username': 'student',
            'password': 'student'
        }
        self.list_msg = []
        self.credentials = pika.PlainCredentials(self.config['username'], self.config['password'])

        # CORECTARE: Toate proprietățile transmise într-un singur obiect de conexiune
        self.parameters = pika.ConnectionParameters(
            host=self.config['host'],
            port=self.config['port'],
            credentials=self.credentials
        )

class RabbitMqProducer(RabbitMqInterface):
    def __init__(self, exchange: str, routing_key: str):
        super().__init__()
        self.config["exchange"] = exchange
        self.config["routing_key"] = routing_key

    def send_message(self, message):
        # automatically close the connection
        with pika.BlockingConnection(self.parameters) as connection:
            # automatically close the channel
            with connection.channel() as channel:
                # CORECTARE: Declarăm mai întâi exchange-ul ca să nu mai dea eroarea 404
                if self.config['exchange']:
                    channel.exchange_declare(
                        exchange=self.config['exchange'],
                        exchange_type='direct',
                        durable=False
                    )

                channel.basic_publish(exchange=self.config['exchange'],
                                      routing_key=self.config['routing_key'],
                                      body=message)


class RabbitMqConsumer(RabbitMqInterface):
    def __init__(self, rabbit_queue: str):
        super().__init__()
        self.config["queue"] = rabbit_queue
        self.connection = pika.BlockingConnection(self.parameters)
        self.channel = self.connection.channel()

        # 1. Declarăm coada
        self.channel.queue_declare(queue=self.config["queue"], durable=False)

        # 2. Declarăm și exchange-ul de siguranță
        self.channel.exchange_declare(exchange="testbidder.direct", exchange_type="direct", durable=False)

        # 3. Legăm coada de exchange utilizând un routing key potrivit (sau numele cozii ca fallback)
        # Majoritatea cozilor din lab folosesc numele cozii ca routing key în configurări, sau o cheie specifică
        routing_key = self.config["queue"].replace(".queue", ".routingkey").replace("processor.queue", "processor.key")
        self.channel.queue_bind(exchange="testbidder.direct", queue=self.config["queue"], routing_key=routing_key)

        # 4. Purjăm coada curat
        self.channel.queue_purge(self.config["queue"])