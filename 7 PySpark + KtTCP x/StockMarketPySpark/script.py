from pyspark import SparkContext
from pyspark.streaming import StreamingContext
import json

sc = SparkContext("local[2]", "Application")
sc.setLogLevel("ERROR") # Ascunde logurile de tip WARN repetitive pentru o consolă curată
ssc = StreamingContext(sc, 3)

lines = ssc.socketTextStream("localhost", 9999)

def calculeaza_profit(line):
    try:
        obj = json.loads(line)
        
        # Verificăm defensiv dacă obiectul conține cheile necesare cerute de formulă
        if all(k in obj for k in ["symbol", "targetMean", "targetLow"]):
            # Ne asigurăm că datele sunt numerice și targetLow nu este zero sau None
            if obj["targetLow"] and obj["targetLow"] > 0 and obj["targetMean"]:
                profit_mediu = obj["targetMean"] - obj["targetLow"]
                procent_profit = (profit_mediu / obj["targetLow"]) * 100
                
                return {
                    "symbol": obj["symbol"],
                    "profit_mediu": profit_mediu,
                    "procent_profit": procent_profit,
                    "valid": True
                }
    except Exception:
        pass
    return {"valid": False}

# 1. Parsăm și reținem doar mesajele care au trecut validarea (au cheile matematice)
parsed_stream = lines.map(calculeaza_profit).filter(lambda x: x["valid"])

# 2. Filtrăm rezultatele păstrându-le doar pe cele pentru care procentul de profit > 40%
filtered_stream = parsed_stream.filter(lambda x: x["procent_profit"] > 40)

# 3. Mapăm fiecare RDD la formatul final cerut: (companie, profitul mediu)
# Folosim .map(), NU .flatMap() pentru a menține tuplul intact
avg_profit = filtered_stream.map(lambda x: (x["symbol"], x["profit_mediu"]))

avg_profit.pprint()

ssc.start()
ssc.awaitTermination()