from datetime import datetime
from random import randint
from uuid import uuid4

from RabbitPC import *

config_bid_producer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte',
    'queue': 'okazii.queue_oferte'
}

config_result_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'key_result',
    'queue': 'okazii.queue_result'
}

class Bidder:
    def __init__(self):
        # producatorul pentru oferte de licitatie
        self.bid_producer = ProducerBidder(config_bid_producer)

        # consumatorul pentru rezultatul licitatiei
        self.result_consumer = ConsumerBidder(config_result_consumer)

        self.my_bid = randint(1000, 10000)  # se genereaza oferta ca numar aleator intre 1000 si 10.000
        self.my_id = uuid4()  # se genereaza un identificator unic pentru ofertant
        self.my_timestamp = datetime.now().second


    def bid(self):
        # se construieste mesajul pentru licitare
        print("Trimit licitatia mea: {}...".format(self.my_bid))

        # se trimite licitatia sub forma de mesaj
        self.bid_producer.send(str(self.my_id) + '|' + str(self.my_bid) + '|' + str(self.my_timestamp))

        # exista o sansa din 2 ca oferta sa fie trimisa de 2 ori pentru a simula duplicatele
        if randint(0, 1) == 1:
            self.bid_producer.send(str(self.my_id) + '|' + str(self.my_bid) + '|' + str(self.my_timestamp))

    def get_winner(self):
        # se asteapta raspunsul licitatiei
        print("Astept rezultatul licitatiei...")
        result = self.result_consumer.getMessage()

        # se verifica identitatea castigatorului

        identity = result.split('|')[0]

        if identity == str(self.my_id):
            print("[{}] Am castigat!!!".format(self.my_id))
        else:
            print("[{}] Am pierdut...".format(self.my_id))

    def run(self):
        self.bid()
        self.get_winner()


if __name__ == '__main__':
    # bids_topic = "topic_oferte",
    # result_topic = "topic_rezultat"
    bidder = Bidder()
    bidder.run()
