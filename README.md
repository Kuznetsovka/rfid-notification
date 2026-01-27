**Запуск локально**
1. Скопировать файл `default-application.yml` в `application.yml`
2. Скопировать файл `default-logback-spring.xml` в `logback-spring.xml`
3. В `application.yml` меняем настройки на локальные. Пример см. в разделе Настройки
4. Запустить NotificationServerApplication 

**Для разработки**

1. После изменений требуется изменить версию в pom.xml файле.
2. Пересоздать образ docker локально.
```bash
mvn clean install
```
3. Пересоздать образ и отправить в docker-hub
```bash
mvn clean install -Dskip.docker.push=false

```
```
docker push kuznetsovka/notification-logger:latest

```


