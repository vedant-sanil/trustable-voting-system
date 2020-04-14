# Voting Client API

## Overview

This API specifies all types of messages that a client node shall handle in addition to the messages that a regular blockchain node and server handle.


The types of messages include the following.

------

## StartVote

**Description**: Cast a vote to the requested candidate after the appropriate encryptions in place.

**Note:** You can refer to the CastVote API in the Server.md to create your encrypted vote object.

### request


**URL**: `/startvote`

**Method**: `POST`

**Body** :

```json
{
    "chain_id": 2,
    "vote_for": "xxx"
}
```
`xxx`: Denotes that username of the candidate that the client should vote for.


File: message/StartVoteRequest.java

### response_1

**Status** : `200 OK`

**Body** :

```json
{
    "success": true
}
```

File: message/StatusReply.java