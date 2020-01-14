# Zurich recycling Telegram bot
[![Build Status](http://jenkins.felunka.de:8080/job/zurich_recycling_telegram_bot/badge/icon)](http://jenkins.felunka.de:8080/job/zurich_recycling_telegram_bot/)

## Idea
In Zurich you must bring out paper, cardboard etc. the day before it will be picked up and I allways forget it. This bot will remind you to bring out the recycling types you have subscribed to the day before at 8pm.

## Usage
Go to your Telegram App, search for `@zuri_trash_bot` and type `/start`.
To determine, if you have to bring out something you will need to add your zip code:
```
You: /location
Bot: Tell me your zip code.
You: 8002
```
Next you must subscribe to at least one group:
```
You: /sub
Bot: Tell me, what you want to subscribe to.
You: paper
```
Type `/list` to list all groups available and `/mysub` to see what you have subscribed.
You can use `/unsub` to remove a subscription.
