import func_timeout

from RabbitPC import *

config_bids_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte',
    'queue': 'okazii.queue_oferte'
}

config_bids_producer_for_messageprocessor = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_oferte_message_processor',
    'queue': 'okazii.queue_oferte_message_processor'
}

config_notify_message_processor_producer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_notificari',
    'queue': 'okazii.queue_notificari'
}


class Auctioneer:
    def __init__(self):
        # consumatorul pentru ofertele de la licitatie
        self.bids_consumer = ConsumerAuctioneer(config_bids_consumer)
        # producatorul pentru notificarea procesorului de mesaje
        self.notify_processor_producer = ProducerAuctioneer(config_notify_message_processor_producer)
        self.bids_producer = ProducerAuctioneer(config_bids_producer_for_messageprocessor)

    def receive_bids(self):
        print("Astept oferte pentru licitatie...")

        try:
            self.bids_consumer.consume()  # populam lista de bid-uri
        except func_timeout.exceptions.FunctionTimedOut:
            pass

        for msg in self.bids_consumer.getQueue():  # ID|12031203|time
            (identity, amount, timestamp) = msg.split('|')
            print("{} a licitat {}".format(identity, amount))
            #le punem la loc pentru MessageProcessor
            self.bids_producer.send(msg)

        #self.notify_processor_producer.send("incheiat")
        self.bids_producer.send('incheiat')


if __name__ == '__main__':
    # bids_topic = "topic_oferte",
    # notify_message_processor_topic = "topic_notificare_procesor_mesaje"
    auctioneer = Auctioneer()
    auctioneer.receive_bids()
