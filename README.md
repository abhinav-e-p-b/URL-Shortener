<img src="https://cdn.prod.website-files.com/677c400686e724409a5a7409/6790ad949cf622dc8dcd9fe4_nextwork-logo-leather.svg" alt="NextWork" width="300" />

# URL Shortener System Design

---

![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_kqswhxb2)

## Project Overview

### What we're building and why

In this project, I’m building a scalable URL shortener using Flask, PostgreSQL, Redis, and Docker so that I can learn how backend services, databases, caching, and containerized applications work together in a real-world system.

## Setting Up the Environment

### Environment setup goals

In this step, I’m setting up my development environment with a terminal, Docker Desktop, and Cursor so that I can build, run, and manage the different services of my URL shortener project.

![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_qj4lc3tb)

### The three core services

The three services are Flask API, PostgreSQL, and Redis. The first handles API requests and URL redirection. The second stores the URL mappings and application data. The third provides fast caching for frequently accessed URLs.


## Designing the System Architecture

### Architecture design goals

In this step, I’m creating the Docker Compose configuration, container setup, Python dependencies, and PostgreSQL database schema so that I can define and run the three-service URL shortener architecture consistently.


![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_pnungqem)

### Services defined in docker-compose.yml

The three services are web, db, and Redis. Each one handles a specific part of the URL shortener: web handles API requests and redirects, db stores URL mappings permanently in PostgreSQL, and Redis provides fast caching for frequently accessed URLs.

![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_ymp8agg2)

### Infrastructure file deep dive

I chose PostgreSQL as the database. Its role is to store the URL mappings permanently, including the unique ID, original long URL, and creation timestamp, so the data remains available even when the application restarts.


## Building the URL Shortener Logic

### Application logic goals

In this step, I’m writing the Flask API logic in `app.py` so that I can create short URLs from long URLs, redirect users to the original URLs, and connect the API with PostgreSQL and Redis.

### The URL shortening flow

When a long URL hits /shorten, the app first stores the URL in PostgreSQL and gets a unique database ID, then it converts that ID into a Base62 short code and caches the mapping in Redis, and finally returns the generated short URL and short code to the user.

![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_seusouy2)

### Handling redirect lookups

When someone visits a short URL, the app first checks Redis for the short code to quickly find the original URL. If that misses, it decodes the short code into the database ID, queries PostgreSQL for the original URL, and then redirects the user to it.

## Launching and Verifying the System

### Launch goals

In this step, I’m starting the Flask API, PostgreSQL database, and Redis cache using Docker Compose so that I can run all three services together and verify that they are healthy and communicating correctly.


![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_3w4aoken)

### Health endpoint response

When I ran curl http://localhost:5000/health, I saw:

{"error":"Short URL not found"}

This happened because the /health endpoint was not defined, so Flask treated health as a short_code using the /<short_code> route.

## Testing and Observing the System

### Testing goals

In this step, I'm testing URL creation, redirection, and cache behavior so that I can observe how Base62 short codes are generated, how redirects work with HTTP status codes, and how the cache-aside pattern retrieves data from Redis before falling back to PostgreSQL.

### Redis cache flush behavior

After flushing Redis, the app had to read the original link from database instead of the Redis cache

## Implementing Rate Limiting with Redis

![Image](https://nextwork.ai/ecstatic_white_trusty_gecko/uploads/02a35a5b-1d8b-43ee-9982-6ceef44145e5_i71om2um)

### Why Redis excels at rate limiting

In this project extension, Redis is better for rate limiting because it is an in-memory store with very fast reads/writes, supports atomic counter operations, and can automatically expire keys using TTL. This makes it ideal for tracking request counts over short time windows without putting extra load on PostgreSQL.

## Reflections and Key Takeaways

### Tools and concepts learned

The key tools I used include Python Flask, PostgreSQL, Redis, Docker, and Docker Compose. Key concepts I learnt include REST APIs, Base62 encoding, database persistence, caching, cache-aside architecture, HTTP redirects, health checks, and multi-service container orchestration.

### Time and challenges

This project took me approximately one hour. The most challenging part was running and verifying.

### Final thoughts

I did this project today to learn how to build and run a multi-service backend application using Flask, PostgreSQL, Redis, and Docker. Another skill I want to learn is deploying and scaling backend applications in the cloud.

---
