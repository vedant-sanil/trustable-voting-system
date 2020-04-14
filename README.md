# Distributed Ledger

Please read the writeup and the API documents for a description of the project.


## How to run tests

1. Modify necessary fields in `test/Config.java`.

2. Run checkpoint tests:
```
make checkpoint
```

3. Run final tests:
```
make test
```

4. Run blockchain speed test:
```
make speed
```

## Handy tool to test server connection
```
# command format:
curl -v -X POST localhost:7001/getchain -d '{JSON}'
# for example:
curl -v -X POST localhost:7001/getchain -d '{"chain_id":1}'
```