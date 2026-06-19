import socket
import sys
import time
from datetime import date
import requests
import json

# Preluăm simbolurile companiilor
print("Preluare simboluri...")
response = requests.get('https://finnhub.io/api/v1/stock/symbol?exchange=US&token=brmr2kfrh5rcss140jmg')
parsed_symbols = json.loads(response.text)

# Pentru teste, limităm la primele 20 de simboluri ca să nu atingem imediat limitarea API
test_symbols = parsed_symbols[:20]

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ('localhost', 9999)
print(f'Serverul pornește pe {server_address[0]} portul {server_address[1]}', file=sys.stderr)
sock.bind(server_address)
sock.listen(1)

while True:
    print('Așteptare conexiune client...', file=sys.stderr)
    connection, client_address = sock.accept()

    try:
        print('Conexiune acceptată de la:', client_address, file=sys.stderr)

        for symbol in test_symbols:
            sym = symbol['symbol']
            today_str = date.today().strftime('%Y-%m-%d')
            
            # Cerere știri pentru simbolul curent
            news_url = f'https://finnhub.io/api/v1/company-news?symbol={sym}&from={today_str}&to={today_str}&token=brmr2kfrh5rcss140jmg'
            news_response = requests.get(news_url)
            
            # Respectăm limita API (max 60 cereri/min -> ~1 cerere la 1-2 secunde)
            time.sleep(1.5) 
            
            if news_response.status_code != 200:
                continue

            parsed_news = json.loads(news_response.text)

            for piece_of_news in parsed_news:
                # Serializare curată în format JSON + separatorul standard de linie '\n'
                json_data = json.dumps(piece_of_news) + "\n"
                connection.sendall(bytes(json_data, encoding='utf8'))
                
                print(f"Trimis știre pentru {sym}")
                # Cerința: o știre la fiecare 3 secunde
                time.sleep(3)

    except Exception as e:
        print(f"Eroare: {e}", file=sys.stderr)
    finally:
        connection.close()