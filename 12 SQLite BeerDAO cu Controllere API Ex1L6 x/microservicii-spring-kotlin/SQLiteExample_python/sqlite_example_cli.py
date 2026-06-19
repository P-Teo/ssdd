import pika
from retry import retry


class RabbitMq:
    config = {
        'host': 'localhost',
        'port': 5678,
        'username': 'student',
        'password': 'student',
        'response_queue': 'queue.response'
    }
    credentials = pika.PlainCredentials(config['username'], config['password'])
    
    # Un singur ConnectionParameters corect
    parameters = pika.ConnectionParameters(
        host=config['host'],
        port=config['port'],
        credentials=credentials
    )

    def on_received_message(self, blocking_channel, deliver, properties, message):
        result = message.decode('utf-8')
        try:
            print(result)
        except Exception:
            print("wrong data format")
        finally:
            blocking_channel.stop_consuming()

    @retry(pika.exceptions.AMQPConnectionError, delay=5, jitter=(1, 3))
    def receive_message(self):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_consume(
                    self.config['response_queue'],
                    self.on_received_message
                )
                try:
                    channel.start_consuming()
                except pika.exceptions.ConnectionClosedByBroker:
                    print("Connection closed by broker.")
                except pika.exceptions.AMQPChannelError:
                    print("AMQP Channel Error")
                except KeyboardInterrupt:
                    print("Application closed.")

    def send_message(self, message):
        with pika.BlockingConnection(self.parameters) as connection:
            with connection.channel() as channel:
                channel.basic_publish(
                    exchange='',
                    routing_key='queue.gateway',
                    body=message
                )


def print_menu():
    print('0 --> Exit program')
    print('1 --> addBeer')
    print('2 --> getBeers')
    print('3 --> getBeerByName')
    print('4 --> getBeerByPrice')
    print('5 --> updateBeer')
    print('6 --> deleteBeer')
    return input("Option=")


if __name__ == '__main__':
    rabbit_mq = RabbitMq()
    rabbit_mq.send_message("createBeerTable~")
    while True:
        option = print_menu()
        if option == '0':
            break
        elif option == '1':
            name = input("Beer name: ")
            price = float(input("Beer price: "))
            rabbit_mq.send_message("addBeer~id=-1;name={};price={}".format(name, price))
        elif option == '2':
            rabbit_mq.send_message("getBeers~")
            rabbit_mq.receive_message()
        elif option == '3':
            name = input("Beer name: ")
            rabbit_mq.send_message("getBeerByName~name={}".format(name))
            rabbit_mq.receive_message()
        elif option == '4':
            price = float(input("Beer price: "))
            rabbit_mq.send_message("getBeerByPrice~price={}".format(price))
            rabbit_mq.receive_message()
        elif option == '5':
            id = int(input("Beer ID: "))
            name = input("Beer name: ")
            price = float(input("Beer price: "))
            rabbit_mq.send_message("updateBeer~id={};name={};price={}".format(id, name, price))
            rabbit_mq.receive_message()
        elif option == '6':
            name = input("Beer name: ")
            rabbit_mq.send_message("deleteBeer~name={}".format(name))
        else:
            print("Invalid option")