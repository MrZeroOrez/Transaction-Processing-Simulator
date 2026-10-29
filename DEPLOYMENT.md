# Render and Neon setup

Root Dockerfile builds React into the Spring Boot JAR. One service serves UI and API on the same origin, without a frontend API URL or CORS change. Select the Free compute plan; no paid disks/services are declared.

Create a separate Neon database for each project. In Render, use the repository root and Docker runtime, or New Blueprint with render.yaml. DATABASE_URL must be `jdbc:postgresql://HOST/DATABASE?sslmode=require`; put username and password in the separate environment variables. Never commit credentials. Keep KAFKA_ENABLED=false for the initial deploy. Open the resulting Render URL and register a new user.

H2 remains the local default. Do not use file H2 for Render persistence. No Neon account or cloud service is provisioned by these files. PostgreSQL and actual Render deployment require verification before a deployed claim is added to the resume.

# Kafka pipeline

Successful trade/transfer transactions insert an outbox event atomically. Optional relay waits for Kafka acknowledgement before marking sent. Broker failure retains the row for retry and does not undo an already committed trade/transfer. A crash after publish can produce duplicates: consumer deduplicates by event ID in SQL. Reporting projection stores only event metadata, never posts money. Topic: banking.events. Enable only after configuring a broker, topic and credentials.

Set KAFKA_ENABLED=true and KAFKA_BOOTSTRAP_SERVERS. For hosted brokers also set KAFKA_SECURITY_PROTOCOL and SASL environment values. Never put secrets in YAML. Use a different consumer group per project/database. Default mode does not connect to a broker. Render sleeping pauses the relay/consumer; this is not 24x7 processing. Outbox retention, operational metrics, poison-message/DLT policy and broker integration verification are still required before production use.
