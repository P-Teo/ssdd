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

config_notify_message_processor_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_notificari',
    'queue': 'okazii.queue_notificari'
}

config_bids_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte_message_processor',
    'queue': 'okazii.queue_oferte_message_processor'
}

config_processed_bids_producer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte_procesate',
    'queue': 'okazii.queue_oferte_procesate'
}


class MessageProcessor:
    def __init__(self):
        # consumatorul notificarii de la Auctioneer cum ca s-a terminat licitatia
        self.notify_message_processor_consumer = ConsumerMessageProcessor_NOTIFICATIONS(
            config_notify_message_processor_consumer)

        # consumatorul pentru ofertele de la licitatie
        self.bids_consumer = ConsumerMessageProcessor_BIDS(config_bids_consumer)

        # producatorul pentru mesajele procesate
        self.processed_bids_producer = ProducerMessageProcessor(
            config_processed_bids_producer)  # aceeasi ca la auctioneer: config_notify_processor_producer

        # ofertele se pun in dictionar, sub forma de perechi <IDENTITATE_OFERTANT, MESAJ_OFERTA>
        self.bids = dict()

        self.loggerProducer = ProducerLogger(config_logger_producer)
        self.loggerProducer.send('Initializare MessageProcessor - ' + str(datetime.now().date()))

    def get_and_process_messages(self):
        # se asteapta notificarea de la Auctioneer pentru incheierea licitatiei

        # print("Astept notificare de la toate entitatile Auctioneer pentru incheierea licitatiei...")
        # auction_end_message = self.notify_message_processor_consumer.getMessage()

        # a ajuns prima notificare, se asteapta si celelalte notificari timp de maxim 15 secunde
        # try:
        #     self.notify_message_processor_consumer.consume() # consuma pentru 15 secunde
        # except func_timeout.exceptions.FunctionTimedOut:
        #     pass

        # for auction_end_message in self.notify_message_processor_consumer.getQueue():
        #     pass

        #try:
        #    print("Incepem consume pe coada msg proc:")
        self.loggerProducer.send('Se consuma bid-urile primite de la Auctioneer - ' + str(datetime.now().date()))
        self.bids_consumer.consume()
        #except func_timeout.exceptions.FunctionTimedOut:
        #    pass

        # se preiau toate ofertele din topicul bids_topic si se proceseaza
        print("Licitatie incheiata. Procesez mesajele cu oferte...")

        for msg in self.bids_consumer.getQueue():
            identity = msg.split('|')[0]  # id|amount|time

            # eliminare duplicate
            if identity not in self.bids.keys():
                self.bids[identity] = msg

        self.loggerProducer.send('Eliminare duplicate - ' + str(datetime.now().date()))

        # sortare dupa timestamp
        sorted_bids = sorted(self.bids.values(), key=lambda bid: bid.split('|')[2])
        self.loggerProducer.send('Sortare timestamp - ' + str(datetime.now().date()))

        self.finish_processing(sorted_bids)
        self.loggerProducer.send('Terminare procesare si trimitere la BiddingProcessor - ' + str(datetime.now().date()))
    def finish_processing(self, sorted_bids):
        print("Procesarea s-a incheiat! Trimit urmatoarele oferte:")

        for bid in sorted_bids:
            (identity, bid_amount, timestamp) = bid.split('|')
            print("[{}] {} a licitat {}.".format(datetime.fromtimestamp(float(timestamp) / 1000), identity, bid_amount))

            # se stocheaza mesajele ordonate dupa timestamp si fara duplicate intr-un topic separat
            self.processed_bids_producer.send(str(identity) + '|' + str(bid_amount) + '|' + str(timestamp))

    def run(self):
        self.get_and_process_messages()


if __name__ == '__main__':
    # bids_topic = "topic_oferte",
    # notify_message_processor_topic = "topic_notificare_procesor_mesaje",
    # processed_bids_topic = "topic_oferte_procesate"

    message_processor = MessageProcessor()
    message_processor.run()
