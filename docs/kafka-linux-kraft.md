# Kafka — команды и пояснения (Linux, KRaft)

Краткая шпаргалка для работы с **Apache Kafka** на Linux в режиме **KRaft** (без ZooKeeper). В **каждом** блоке команд ниже задаётся `export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"` и скрипты вызываются как `"$KAFKA_HOME/bin/…"`. При необходимости измените путь под свою версию и каталог установки.

В современных дистрибутивах скрипты могут быть без суффикса `.sh` — используйте то, что есть в `$KAFKA_HOME/bin`.

---

## KRaft в двух словах

- **Metadata** (кто брокер, какие топики, конфигурации) хранится во внутреннем топике `__cluster_metadata`, а не в ZooKeeper.
- Первый запуск кластера требует **форматирования каталога данных** и **cluster ID** (один раз на узел).
- Для локальной разработки часто запускают **один брокер** с `process.roles=broker,controller`.

Подробности — в официальной документации: [KRaft](https://kafka.apache.org/documentation/#kraft).

---

## Инициализация хранилища (первый старт брокера KRaft)

Перед первым запуском брокера нужно отформатировать `log.dirs` указанным **cluster ID**.

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# сгенерировать cluster ID (один на весь кластер)
"$KAFKA_HOME/bin/kafka-storage.sh" random-uuid
```

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# подставьте UUID из предыдущей команды и путь к вашему server.properties
"$KAFKA_HOME/bin/kafka-storage.sh" format -t <CLUSTER_UUID> -c /path/to/server.properties
```

**Зачем:** без `format` брокер не стартует в KRaft — в каталоге данных нет нужной разметки метаданных.

**Повторный format** на уже заполненном диске опасен (можно потерять данные). Для «чистого» стенда удаляйте содержимое `log.dirs` только осознанно.

---

## Запуск и остановка

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# обычно так (имя файла конфигурации свой)
"$KAFKA_HOME/bin/kafka-server-start.sh" /path/to/server.properties
```

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# в foreground; для фона — systemd, supervisor, screen/tmux
"$KAFKA_HOME/bin/kafka-server-stop.sh"
```

**Пояснение:** `kafka-server-start.sh` поднимает процесс брокера (и контроллер, если роли объединены в одном конфиге).

---

## Топики

### Список топиков

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-topics.sh" --bootstrap-server localhost:9092 --list
```

### Описание топика (партиции, реплики, ISR)

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-topics.sh" --bootstrap-server localhost:9092 --describe --topic orders-events
```

### Создать топик

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-topics.sh" --bootstrap-server localhost:9092 \
  --create --topic orders-events \
  --partitions 3 --replication-factor 1
```

**Замечание:** `--replication-factor` не может быть больше числа брокеров. На одном брокере обычно `1`.

### Удалить топик

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-topics.sh" --bootstrap-server localhost:9092 --delete --topic orders-events
```

Нужно, чтобы в кластере было разрешено удаление (`delete.topic.enable=true` на брокерах — в новых версиях часто по умолчанию разрешено).

### Изменить конфигурацию топика (например, retention)

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-configs.sh" --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name orders-events \
  --alter --add-config retention.ms=86400000
```

---

## Запись и чтение сообщений (отладка)

### Консольный продюсер

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-console-producer.sh" --bootstrap-server localhost:9092 --topic orders-events
```

После запуска вводите строки — каждая строка станет **значением** сообщения (value). Для ключа:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-console-producer.sh" --bootstrap-server localhost:9092 \
  --topic orders-events \
  --property "parse.key=true" \
  --property "key.separator=:"
```

Пример строки: `my-key:{"hello":"world"}`

### Консольный консьюмер (с начала)

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-console-consumer.sh" --bootstrap-server localhost:9092 \
  --topic orders-events \
  --from-beginning
```

### Консольный консьюмер с группой

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-console-consumer.sh" --bootstrap-server localhost:9092 \
  --topic orders-events \
  --group debug-cli-group
```

**Пояснение:** без `--from-beginning` группа читает только **новые** сообщения после подключения.

---

## Группы потребителей

### Список групп

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-consumer-groups.sh" --bootstrap-server localhost:9092 --list
```

### Смещения и лаг

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-consumer-groups.sh" --bootstrap-server localhost:9092 \
  --describe --group orders-ms
```

### Сброс смещений (осторожно в проде)

Примеры:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# к самому новому (конец)
"$KAFKA_HOME/bin/kafka-consumer-groups.sh" --bootstrap-server localhost:9092 \
  --group orders-ms --topic orders-events --reset-offsets --to-latest --execute

# к самому старому (начало)
"$KAFKA_HOME/bin/kafka-consumer-groups.sh" --bootstrap-server localhost:9092 \
  --group orders-ms --topic orders-events --reset-offsets --to-earliest --execute
```

Сначала можно выполнить без `--execute`, чтобы увидеть план.

---

## Кластер и метаданные KRaft

### Кто контроллер / обзор брокеров

В зависимости от версии Kafka:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-metadata-quorum.sh" --bootstrap-server localhost:9092 describe --status
```

или (в некоторых сборках):

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-broker-api-versions.sh" --bootstrap-server localhost:9092
```

Точное имя скрипта зависит от версии; при сомнении:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
ls "$KAFKA_HOME/bin" | grep -i metadata
```

### Metadata shell (чтение снимка с диска контроллера)

Используется для глубокой диагностики; путь к снимку берётся из данных контроллера:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-metadata-shell.sh" --snapshot /path/to/__cluster_metadata-0/00000000000000000000.log
```

**Пояснение:** это не повседневная команда, а инструмент для разборов проблем с метаданными.

---

## ACL (если включена безопасность)

Пример (варьируется по версии и SASL/SSL):

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
"$KAFKA_HOME/bin/kafka-acls.sh" --bootstrap-server localhost:9092 \
  --list
```

Настройка прав выходит за рамки этой шпаргалки.

---

## Частые проблемы

| Симптом | Что проверить |
|--------|----------------|
| `UnknownTopicOrPartitionException` | Топик не создан или опечатка в имени; для Spring — см. `spring.kafka` и placeholder свойств. |
| `Leader not available` | Не все брокеры подняты; только что созданный топик ждёт выборов лидера. |
| Нельзя создать топик с RF=3 на одном брокере | Уменьшите `--replication-factor`. |
| Консьюмер «ничего не видит` | Нет `--from-beginning`, группа уже закоммитила оффсеты; или продюсер в другой топик/кластер. |

---

## Kafka в Docker

Если брокер в контейнере, команды выполняют внутри контейнера:

```bash
export KAFKA_HOME="$HOME/kafka/kafka_2.13-4.0.2"
# внутри контейнера путь к бинарям свой (пример для образа Bitnami):
docker exec -it <kafka-container-name> /opt/bitnami/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

На **хосте** для той же команды используйте `"$KAFKA_HOME/bin/kafka-topics.sh"` после `export`, как в разделах выше.

`localhost` внутри контейнера обычно указывает на сам брокер; с хоста используйте опубликованный порт и иногда advertised listeners — см. `docker-compose` вашего стенда.

---

## Полезные ссылки

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Operations — KRaft](https://kafka.apache.org/documentation/#kraft_operations)
