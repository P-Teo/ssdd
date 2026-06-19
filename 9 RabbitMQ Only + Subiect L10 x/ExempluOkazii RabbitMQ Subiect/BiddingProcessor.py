from datetime import datetime

import func_timeout

from RabbitPC import *

config_logger_producer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_log',
    'queue': 'okazii.queue_log'
}

config_processed_bids_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte_procesate',
    'queue': 'okazii.queue_oferte_procesate'
}

config_result_producer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_result',
    'queue': 'okazii.queue_result'
}


class BiddingProcessor:
    def __init__(self):

        # consumatorul pentru ofertele procesate
        self.processed_bids_consumer = ConsumerBiddingProcessor(config_processed_bids_consumer)

        # producatorul pentru trimiterea rezultatului licitatiei
        self.result_producer = ProducerBiddingProcessor(config_result_producer)

        self.loggerProducer = ProducerLogger(config_logger_producer)

        self.loggerProducer.send('Initializare BiddingProcessor - ' + str(datetime.now().date()))

    def get_processed_bids(self):

        # se preiau toate ofertele procesate din topicul processed_bids_topic
        print("Astept ofertele procesate de MessageProcessor...")

        # ofertele se stocheaza sub forma de perechi <PRET_LICITAT, MESAJ_OFERTA>
        bids = dict()
        no_bids_available = True

        while no_bids_available:
            self.loggerProducer.send('Consumare bid-uri procesate - ' + str(datetime.now().date()))
            try:
                self.processed_bids_consumer.consume()
            except func_timeout.exceptions.FunctionTimedOut:
                pass

            for msg in self.processed_bids_consumer.getQueue():
                bid_amount = msg.split('|')[1]
                bids[bid_amount] = msg
                self.loggerProducer.send('Populare dictionar cu identitate si suma - ' + str(datetime.now().date()))

            # daca inca nu exista oferte, se asteapta in continuare
            if len(bids) != 0:
                no_bids_available = False

        self.decide_auction_winner(bids)
        self.loggerProducer.send('Decidere castigatori - ' + str(datetime.now().date()))

    def decide_auction_winner(self, bids):
        print("Procesez ofertele...")

        if len(bids) == 0:
            print("Nu exista nicio oferta de procesat.")
            return

        # sortare dupa oferte, descrescator
        sorted_bids = sorted(bids.keys(), reverse=True)

        # castigatorul este ofertantul care a oferit pretul cel mai mare
        winner = bids[sorted_bids[0]]

        winner_identity = winner.split('|')[0]

        print("Castigatorul este:")
        print("\t{} - pret licitat: {}".format(winner_identity, sorted_bids[0]))

        # se trimite rezultatul licitatiei pentru ca entitatile Bidder sa il preia din topicul corespunzator
        for useless_index in bids:
            self.result_producer.send(str(winner_identity) + '|' + str(sorted_bids[0]))

    def run(self):
        self.get_processed_bids()


if __name__ == '__main__':
    # processed_bids_topic = "topic_oferte_procesate",
    # result_topic = "topic_rezultat"
    bidding_processor = BiddingProcessor()
    bidding_processor.run()
