import threading
from argparse import ArgumentParser
from datetime import datetime

import grpc

from gen import chat_pb2
from gen import chat_pb2_grpc

DEFAULT_HOST = 'localhost'
DEFAULT_PORT = 50051

last_message_id = -1


def receive_loop(channel, user_id, group_id):
    global last_message_id

    stub = chat_pb2_grpc.ChatStub(channel)
    user_info = chat_pb2.UserInfo(
        userId=user_id,
        groupId=group_id
    )
    messages_stream = stub.Join(user_info)

    try:
        for message in messages_stream:
            print(message)

            if message.HasField("id") and message.id > last_message_id:
                last_message_id = message.id
    except Exception:
        print("Join stub couldn't connect to server.")
        return


def send_loop(channel, user_id):
    global last_message_id

    def iterator():
        while True:
            line = input()

            if line == 'exit':
                break

            message = chat_pb2.Message(
                userId=user_id,
                priority=0,
                text=line,
                time=int(datetime.now().timestamp())
            )

            if last_message_id >= 0:
                message.ackId = last_message_id

            yield message

    try:
        stub = chat_pb2_grpc.ChatStub(channel)
        stub.SendMessage(iterator())
    except Exception:
        print("SendMessage stub couldn't connect to server.")


def main(host, port, user_id, group_id):
    global last_message_id

    channel = grpc.insecure_channel(f'{host}:{port}')

    receive_thread = threading.Thread(target=receive_loop, args=(channel, user_id, group_id))
    receive_thread.start()

    send_thread = threading.Thread(target=send_loop, args=(channel, user_id))
    send_thread.start()

    send_thread.join()
    receive_thread.join()

    channel.close()


if __name__ == '__main__':
    args = ArgumentParser()
    args.add_argument('--user', required=True, type=int)
    args.add_argument('--group', required=True, type=int)
    args.add_argument('--host', default=DEFAULT_HOST, type=str)
    args.add_argument('--port', default=DEFAULT_PORT, type=int)
    args = args.parse_args()

    main(args.host, args.port, args.user, args.group)
