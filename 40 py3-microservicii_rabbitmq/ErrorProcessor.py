import time
from RabbitMqConnection import RabbitMqConsumer

class ErrorClassifier:
    """Clasă responsabilă strict de identificarea și clasificarea tipului de eroare."""
    @staticmethod
    def classify(error_msg: str) -> str:
        msg_lower = error_msg.lower()
        if "connection" in msg_lower or "broker" in msg_lower or "socket" in msg_lower:
            return "Erori de Comunicatie (Retea/Broker)"
        elif "queue" in msg_lower or "purge" in msg_lower or "channel" in msg_lower:
            return "Erori de Sistem de Cozi (RabbitMQ)"
        elif "timeout" in msg_lower or "nici o valoare noua" in msg_lower:
            return "Erori de Timeout / Lipsa Mesaje"
        else:
            return "Erori de Aplicatie / Validare"

class StatsAggregator:
    """Clasă responsabilă cu stocarea și agregarea metricilor/statisticilor."""
    def __init__(self):
        self.stats = {
            "Erori de Comunicatie (Retea/Broker)": 0,
            "Erori de Sistem de Cozi (RabbitMQ)": 0,
            "Erori de Timeout / Lipsa Mesaje": 0,
            "Erori de Aplicatie / Validare": 0
        }
        self.total_errors = 0

    def register_error(self, category: str):
        if category in self.stats:
            self.stats[category] += 1
        else:
            self.stats[category] = 1
        self.total_errors += 1

    def get_report(self) -> str:
        report = "=== STATISTICI ERORI LICITATIE ===\n"
        report += f"Total erori inregistrate pana la adjudecare: {self.total_errors}\n"
        report += "---------------------------------\n"
        for category, count in self.stats.items():
            percentage = (count / self.total_errors * 100) if self.total_errors > 0 else 0
            report += f"- {category}: {count} ({percentage:.2f}%)\n"
        report += "=================================\n"
        return report

class ReportWriter:
    """Clasă responsabilă strict de scrierea raportului în fișier local."""
    def __init__(self, filename: str = "statistici_erori.txt"):
        self.filename = filename

    def save(self, content: str):
        with open(self.filename, "w", encoding="utf-8") as file:
            file.write(content)
        print(f"[INFO-ReportWriter] Raportul a fost salvat cu succes in '{self.filename}'.")


class ErrorStatsProcessor:
    """Orchestratorul principal (Microserviciul de Statistica)."""
    def __init__(self, consumer: RabbitMqConsumer, classifier: ErrorClassifier, aggregator: StatsAggregator, writer: ReportWriter):
        self.consumer = consumer
        self.classifier = classifier
        self.aggregator = aggregator
        self.writer = writer

    def process_errors(self):
        print("[INFO-ErrorStatsProcessor] Microserviciul de statistica a pornit si asculta...")
        time.sleep(5)  # Permite celorlalte servicii sa inceapa activitatea

        # Simulam ascultarea continua pana cand coada devine goala sau procesul e oprit extern
        # Nota: Într-un sistem de producție real, s-ar folosi un mesaj de tip "licitatie_adjudecata" pentru oprire.

        stop_processing = False
        retry_count = 0

        while not stop_processing and retry_count < 3:
            try:
                self.consumer.receive_message()
            except Exception as e:
                # Daca insusi consumer-ul esueaza la conexiune, clasificam eroarea interna
                category = self.classifier.classify(str(e))
                self.aggregator.register_error(category)

            if len(self.consumer.list_msg) != 0:
                retry_count = 0 # resetam daca primim mesaje
                while len(self.consumer.list_msg) != 0:
                    msg = self.consumer.list_msg.pop()

                    # Decuplarea si curatarea mesajului primit de la celelalte microservicii
                    clean_msg = msg.replace("except:", "")

                    # Clasificam si agregam
                    category = self.classifier.classify(clean_msg)
                    self.aggregator.register_error(category)
            else:
                # Daca nu mai sunt mesaje in coada, asteptam putin (simulam finalul licitatiei)
                time.sleep(2)
                retry_count += 1

        # Generam si salvam raportul final
        final_report = self.aggregator.get_report()
        print("\n" + final_report)
        self.writer.save(final_report)

    def run(self):
        self.process_errors()
        print("[INFO-ErrorStatsProcessor] Activitate finalizata.")


if __name__ == "__main__":
    # Instantierea componentelor si injectarea dependentelor (DIP din SOLID)
    rabbitmq_consumer = RabbitMqConsumer(rabbit_queue="errorprocessor.queue")
    error_classifier = ErrorClassifier()
    stats_aggregator = StatsAggregator()
    report_writer = ReportWriter("statistici_erori.txt")

    processor = ErrorStatsProcessor(
        consumer=rabbitmq_consumer,
        classifier=error_classifier,
        aggregator=stats_aggregator,
        writer=report_writer
    )
    processor.run()