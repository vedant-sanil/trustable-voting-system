# BlockChainNode

## Overview

This API specifies all types of messages that a blockchain node shall handle.
The types of messages include the following.

1. From users

    1. **GetBlockChain**: Get a copy of the blockchain. Could also be from peers.
    2. **MineBlock**: Mine a new block
    3. **AddBlock**: Add a newly-mined block to the blockchain

2. From peers

    1. **BroadcastBlock**: Broadcast a block to be transacted

3. For testing purpose

    1. **Sleep**: Enter a sleep state for some time.

The configuration for a node should be the fowlloing.

**Host Name**: localhost

**Port**: specified in test/Config.java

In all cases, if the API call receives invalid data (there is error when parse the request), always return `400 Bad Request`.

------

## GetBlockChain

**Description**: Request from users or peers to get the local copy of a blockchain on a node.

### request

**URL**: `/getchain`

**Method**: `POST`

**Body**: 

```json
{
    "chain_id": 1
}
```
`chain_id`: an integer, the id of the requested blockchain. `1` for identity chain, `2` for vote chain.

File: message/GetChainRequest.java


### response_1

**Description**: On success, return the chain.

**Status** : `200 OK`

**Body** : 

```json
{
    "chain_id": 1,
    "chain_length": 1,
    "blocks": [
        {
            "id": 0,
            "data": {
                "public_key": "xxx",
                "user_name": "xxx"
            },
            "timestamp": 5415419034,
            "nonce": 3413,
            "previous_hash": "xxx",
            "hash": "xxx"
        }
    ]
}
```

```json
{
    "chain_id": 2,
    "chain_length": 1,
    "blocks": [
        {
            "id": 0,
            "data": {
                "vote": "xxx",
                "voter_credential": "xxx"
            },
            "timestamp": 5415419034,
            "nonce": 0,
            "previous_hash": "xxx",
            "hash": "xxx"
        }
    ]
}
```

`chain_id`: an integer, the ID of the blockchain. `1` for identity chain, `2` for vote chain.

`chain_length`: an integer, the length of the blockchain. 

`blocks`: an array, all blocks in the blockchain. Each block contains the following fields.

- `id`: a long integer, the ID of the block.
- `data`: a map, with keys and values as strings.
- `timestamp`: a long integer, block creation time, denoted in system time in milliseconds.
- `nonce`: a long integer, the solution to the difficult problem.
- `previous_hash`: a string, the hexadecimal hash string of the previous block.
- `hash`: a string, the hexadecimal hash string of the block.

Files: message/GetChainReply.java, message/Block.java


### response_2

**Description**: Request information is incorrect.

**Status** : `404 Not Found`

**Body** : not specified.


------


## MineBlock

**Description**: Request from a user to mine a new block for a blockchain.

Upon such requests, the node will build a block with the data provided by the user.
Then the node tries to solve the prefix hash problem by brute-force searching for a proper nonce.  

To increase the chance of a new block being approved, it may be a good idea to synchronize with peers and get the latest version of blockchain before mining.

### request

**URL**: `/mineblock`

**Method**: `POST`

**Body**:
```json
{
    "chain_id": 1,
    "data": {
        "public_key": "xxx",
        "user_name": "xxx"
    }
}
```

```json
{
    "chain_id": 2,
    "data": {
        "vote": "xxx",
        "voter_credential": "xxx"
    }
}
```

`chain_id`: an integer, the ID of the blockchain. `1` for identity chain, `2` for vote chain.

`data`: a map, with keys and values as strings.

File: message/MineBlockRequest.java


### response_1

**Description**: On success, return the mined block.

**Code** : `200 OK`

**Body** :

```json
{
    "chain_id": 1,
    "block": {
        "id": 0,
        "data": {
            "public_key": "xxx",
            "user_name": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 3413,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

```json
{
    "chain_id": 2,
    "block": {
        "id": 0,
        "data": {
            "vote": "xxx",
            "voter_credential": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 0,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

`chain_id`: an integer, the ID of the blockchain. `1` for identity chain, `2` for vote chain.

`block`: the newly-mined block. The fields of a block is specified in the GetBlockChain section.

File: message/BlockReply.java


### response_2

**Description**: Request information is incorrect.

**Status** : `404 Not Found`

**Body** : not specified.

------

## AddBlock

**Description**: Request from client to add a block to the blockchain.

If the block is valid, the node will perform a two-phase commit.

1. *PRECOMMIT* phase: Broadcast the block to all peers. If no less than 2/3 of its peers approve the block, proceed to *COMMIT*. Otherwise abort and return failure.
2. *COMMIT* phase: Add the block to the blockchain. Broadcast the block to all peers again. Return success to the user.

### request

**URL**: `/addblock`

**Method**: `POST`

**Body**:

```json
{
    "chain_id": 1,
    "block": {
        "id": 1,
        "data": {
            "public_key": "xxx",
            "user_name": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 3413,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

```json
{
    "chain_id": 2,
    "block": {
        "id": 1,
        "data": {
            "vote": "xxx",
            "voter_credential": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 0,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

`chain_id`: an integer, the ID of the blockchain. `1` for identity chain, `2` for vote chain.

`block`: the block to be added. The fields of a block is specified in the GetBlockChain section.

File: message/AddBlockRequest.java


### response_1

**Description**: On success.

**Code** : `200 OK`

**Body** :

```json
{
    "success": true,
    "info": ""
}
```

`success`: a boolean, true.

`info`: a string, not specified.

File: message/StatusReply.java


### response_2

**Description**: On failure.

**Code** : `409 Conflict`

**Body** :

```json
{
    "success": false,
    "info": ""
}
```

`success`: a boolean, false.

`info`: a string, not specified.

File: message/StatusReply.java


### response_3

**Description**: Request information is incorrect.

**Status** : `404 Not Found`

**Body** : not specified.

------

## BroadcastBlock

**Description**: Broadcast from a node to peers to request adding a block.

### request

**URL**: `/broadcast`

**Method**: `POST`

**Body**:

```json
{
    "chain_id": 1,
    "request_type": "PRECOMMIT",
    "block": {
        "id": 1,
        "data": {
            "public_key": "xxx",
            "user_name": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 3413,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

```json
{
    "chain_id": 2,
    "request_type": "COMMIT",
    "block": {
        "id": 1,
        "data": {
            "vote": "xxx",
            "voter_credential": "xxx"
        },
        "timestamp": 5415419034,
        "nonce": 0,
        "previous_hash": "xxx",
        "hash": "xxx"
    }
}
```

`chain_id`: an integer, the ID of the blockchain. `1` for identity chain, `2` for vote chain.

`request_type`: a string, either "PRECOMMIT" or "COMMIT"

`block`: the block to be broadcast. The fields of a block is specified in the GetBlockChain section.

File: message/BroadcastRequest.java


### response_1

**Description**: On success.

**Code** : `200 OK`

**Body** :

```json
{
    "success": true,
    "info": ""
}
```

`success`: a boolean, true.

`info`: a string, not specified.

File: message/StatusReply.java


### response_2

**Description**: On failure.

**Code** : `409 Conflict`

**Body** :

```json
{
    "success": false,
    "info": ""
}
```

`success`: a boolean, false.

`info`: a string, not specified.

File: message/StatusReply.java


### response_3

**Description**: Request information is incorrect.

**Status** : `404 Not Found`

**Body** : not specified.


------

## Sleep

**Description**: Put a node to sleep state to simulate network disconnection.
This API will only be called for testing purpose.

When received, the node should reply immediately and then go to sleep.

In sleep state, the node should not accept any request that attempts to modify the blockchain. 
Upon such requests, the node may either not respond or respond with failure.
The response for other requests is not specified.


### request

**URL**: `/sleep`

**Method**: `POST`

**Body**:
```json
{
    "timeout": 5
}
```
`timeout`: an integer, the number of seconds the node should sleep.

File: message/SleepRequest.java


### response_1

**Code** : `200 OK`

**Body** : 

```json
{
    "success": true,
    "info": ""
}
```

`success`: a boolean, false.

`info`: a string, not specified.

File: message/StatusReply.java