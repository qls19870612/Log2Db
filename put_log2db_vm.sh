#!/bin/bash
#SFTP������Ϣ
#IP
IP=192.168.1.99
#�˿�
PORT=22
#�û���
USER=root
#����
PASSWORD=song
#���ϴ��ļ���Ŀ¼
CLIENTDIR=D:/workspace/Log2Db
#SFTPĿ¼
SEVERDIR=/home/javas
#���ϴ��ļ���
FILE=Log2Db.jar

#lftp -u ${USER},${PASSWORD} sftp://${IP}:${PORT} <<EOF
curl -v --insecure sftp://${USER}:${PASSWORD}@134.175.127.247
cd ${SEVERDIR}/
lcd ${CLIENTDIR}
put ${FILE}
by
EOF
