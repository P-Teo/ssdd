from tkinter import *
from tkinter import ttk
import threading
import socket

# Adrese microservicii
TEACHER_HOST = "localhost"
TEACHER_PORT = 1600

REGISTRY_HOST = "localhost"
REGISTRY_PORT = 1700


# ─────────────────────────────────────────────
#  Funcții pentru comunicarea cu Teacher (lab8)
# ─────────────────────────────────────────────

def resolve_question(question_text):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((TEACHER_HOST, TEACHER_PORT))
        sock.send(bytes(question_text + "\n", "utf-8"))
        response_text = str(sock.recv(1024), "utf-8")
    except ConnectionError:
        response_text = "Eroare de conectare la microserviciul Teacher!\n"
    finally:
        sock.close()
    response_widget.insert(END, response_text)


def ask_question():
    question_text = question.get()
    threading.Thread(target=resolve_question, args=(question_text,)).start()


# ─────────────────────────────────────────────
#  Funcții registry publish-subscribe (nou)
# ─────────────────────────────────────────────

_registry_sock = None        # socket persistent pentru subscribe


def send_registry_command(command: str, persistent=False):
    """
    Trimite o comandă la DockerRegistryMicroservice.
    Dacă persistent=True, păstrează socket-ul deschis (pentru subscribe + notificări).
    """
    global _registry_sock

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((REGISTRY_HOST, REGISTRY_PORT))
        sock.send(bytes(command + "\n", "utf-8"))

        if persistent:
            _registry_sock = sock
            # Citim confirmarea de subscribe
            ack = sock.makefile().readline()
            registry_log.insert(END, f"[subscribe] {ack}")
            # Ascultăm notificările pe thread separat
            threading.Thread(target=listen_notifications, args=(sock,), daemon=True).start()
        else:
            response = str(sock.recv(4096), "utf-8")
            registry_log.insert(END, f"[{command.split()[0]}] {response}")
            sock.close()

    except ConnectionError as e:
        registry_log.insert(END, f"Eroare conexiune registry: {e}\n")
        if not persistent:
            sock.close()


def listen_notifications(sock):
    """Rulează pe thread separat; primește notificări și le afișează în log."""
    reader = sock.makefile()
    try:
        for line in reader:
            line = line.strip()
            if line:
                # Actualizarea widget-ului trebuie să fie thread-safe — folosim after()
                root.after(0, lambda l=line: registry_log.insert(END, f"[notificare] {l}\n"))
    except Exception:
        pass


def do_subscribe():
    threading.Thread(
        target=send_registry_command,
        args=("subscribe",),
        kwargs={"persistent": True},
        daemon=True
    ).start()


def do_unsubscribe():
    global _registry_sock
    if _registry_sock:
        try:
            _registry_sock.send(b"unsubscribe\n")
            _registry_sock.close()
        except Exception:
            pass
        _registry_sock = None
        registry_log.insert(END, "[unsubscribe] deconectat\n")
    else:
        registry_log.insert(END, "[unsubscribe] nu ești abonat\n")


def do_push():
    name = push_name.get().strip()
    tag = push_tag.get().strip() or "latest"
    if not name:
        registry_log.insert(END, "Introdu un nume de imagine!\n")
        return
    threading.Thread(
        target=send_registry_command,
        args=(f"push {name} {tag}",),
        daemon=True
    ).start()


def do_list():
    threading.Thread(
        target=send_registry_command,
        args=("list",),
        daemon=True
    ).start()


# ─────────────────────────────────────────────
#  Interfață grafică
# ─────────────────────────────────────────────

if __name__ == '__main__':
    root = Tk()
    root.title("Profesor–Studenți + Docker Registry")
    root.columnconfigure(0, weight=1)
    root.rowconfigure(0, weight=1)

    nb = ttk.Notebook(root)
    nb.grid(column=0, row=0, sticky="nsew", padx=8, pady=8)

    # ── Tab 1: Teacher (lab8 original) ──────────────────────────────────────
    tab_teacher = ttk.Frame(nb)
    nb.add(tab_teacher, text="Profesor–Studenți")

    response_widget = Text(tab_teacher, height=10, width=60)
    response_widget.grid(column=0, row=0, columnspan=4, padx=4, pady=4)

    ttk.Label(tab_teacher, text="Profesorul întreabă:").grid(column=0, row=1)
    question = ttk.Entry(tab_teacher, width=40)
    question.grid(column=1, row=1, columnspan=2, padx=4)
    ttk.Button(tab_teacher, text="Întreabă", command=ask_question).grid(column=3, row=1)

    # ── Tab 2: Docker Registry ───────────────────────────────────────────────
    tab_registry = ttk.Frame(nb)
    nb.add(tab_registry, text="Docker Registry")

    # Log
    registry_log = Text(tab_registry, height=12, width=60, state=NORMAL)
    registry_log.grid(column=0, row=0, columnspan=4, padx=4, pady=4)

    # Subscribe / Unsubscribe
    ttk.Button(tab_registry, text="Subscribe", command=do_subscribe).grid(column=0, row=1, padx=4, pady=4)
    ttk.Button(tab_registry, text="Unsubscribe", command=do_unsubscribe).grid(column=1, row=1, padx=4, pady=4)

    # Push
    ttk.Label(tab_registry, text="Imagine:").grid(column=0, row=2, sticky=E)
    push_name = ttk.Entry(tab_registry, width=20)
    push_name.grid(column=1, row=2, padx=4)

    ttk.Label(tab_registry, text="Tag:").grid(column=2, row=2, sticky=E)
    push_tag = ttk.Entry(tab_registry, width=10)
    push_tag.insert(0, "latest")
    push_tag.grid(column=3, row=2, padx=4)

    ttk.Button(tab_registry, text="Push", command=do_push).grid(column=0, row=3, columnspan=2, pady=4)

    # List
    ttk.Button(tab_registry, text="List images", command=do_list).grid(column=2, row=3, columnspan=2, pady=4)

    root.mainloop()