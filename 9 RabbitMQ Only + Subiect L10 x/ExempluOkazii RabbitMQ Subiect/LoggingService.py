from RabbitPC import *
config_logger_consumer = {
    'host': '127.0.0.1',
    'port': 5678,
    'username': 'student',
    'password': 'student',
    'exchange': 'okazii.exchange',
    'routing_key': 'okazii.key_log',
    'queue': 'okazii.queue_log'
}

class LoggingService:
    def __init__(self):
        self.consumer_log = ConsumerLogger(config_logger_consumer)

    def log(self):
        self.consumer_log.consume()

if __name__ == '__main__':
    logger = LoggingService()
    logger.log()
