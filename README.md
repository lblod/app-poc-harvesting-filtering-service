# [HARVESTING-SELF-SERVICE]: Validation Task

## !IMPORTANT

Version 0.2.x and onward introduces the split files feature (each downloaded pages is treated as a separate model),
major breaking changes with 0.1.x are expected.

If you don't use the latest version of the harvester (master branch), please do not upgrade.

Validate and filter based on a single shacl application profile.

- React to delta
- Validate the graph fetched from the input container
- Filter the errored triples
- Generate a https://www.w3.org/ns/shacl#ValidationReport

## Setup using docker-compose

- Create a new directory `./config/validation`
- Add your Shacl profile `./config/validation/application-profile.ttl`
- Add the service to your docker-compose:

```
  validation:
    image: lblod/app-poc-harvesting-filtering-service
    volumes:
      - ./config/validation:/config
      - ./data/files:/share

```

- add delta rule:

```
  {
    match: {
      predicate: {
        type: 'uri',
        value: 'http://www.w3.org/ns/adms#status'
      },
      object: {
        type: 'uri',
        value: 'http://redpencil.data.gift/id/concept/JobStatus/scheduled'
      }
    },
    callback: {
      method: 'POST',
      url: 'http://validation/delta'
    },
    options: {
      resourceFormat: 'v0.0.1',
      gracePeriod: 1000,
      ignoreFromSelf: true
    }
  }
```

## Environment variables

- `SERVER_PORT` : default set to `80`
- `SHARE_FOLDER_DIRECTORY`: default set to `/share`
- `BATCH_SIZE` : default set to `100`
- `APPLICATION_PROFILE_PATH` : default set to `/config/application-profile.ttl`
- `LOGGING_LEVEL` : default set to `INFO`
- `SPARQL_ENDPOINT` : default set to `http://database:8890/sparql`
- `MAX_REQUEST_SIZE` : default set to `512MB`
- `MAX_FILE_SIZE` : default set to `512MB`
- `TARGET_GRAPH` : default set to `http://mu.semte.ch/application`
- `JAVA_OPTS` : not set by default. e.g `-Xms640M -Xmx1280M`

## Development

In case you want to test a change, but don't have java/maven installed on your machine,
you can use the dummy docker-compose stack provided for this purpose.

- `cd ./test`
- `docker-compose build`
- `docker-compose up`
- Wait that the migration service has finished
- Open postman
- Run the request below
- check the generated files in `./test/filtering`
- check the triplestore at `http://localhost:8891/sparql`

`POST localhost/delta`

```
[
  {
    "inserts": [
      {
        "subject": {
          "type":"",
          "value":"http://redpencil.data.gift/id/task/84ecbc30-7cc9-11eb-b493-2329471b3650",
          "datatype":""
        },
        "predicate": {
          "type":"",
          "value":"http://www.w3.org/ns/adms#status",
          "datatype":""
        },
        "object": {
          "type":"",
          "value":"http://redpencil.data.gift/id/concept/JobStatus/scheduled",
          "datatype":""
        }
      }

      ],
    "deletes": []
  }
]

```
