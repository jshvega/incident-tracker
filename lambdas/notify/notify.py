import json
import logging
import os
import boto3
import sys



log = logging.getLogger()
class JsonFormatter(logging.Formatter):
    def format(self, record):
        entry = {"level": record.levelname, "msg": record.getMessage()}
        entry.update(getattr(record, "fields", {}))   # custom keys
        if record.exc_info:
            entry["exc"] = self.formatException(record.exc_info)
        return json.dumps(entry, default=str)
stream = logging.StreamHandler(sys.stdout)
stream.setFormatter(JsonFormatter())
logging.basicConfig(level=logging.INFO, handlers=[stream], force=True)



s3 = boto3.client("s3")   # module level: created once per container, reused across invocations

def build_record(event: dict) -> tuple[str, dict]:
    detail = event["detail"]
    if detail["schemaVersion"] != 1:
        raise ValueError(f"unsupported schemaVersion {detail["schemaVersion"]}")

    date_raw = event["time"]
    date = date_raw[:10]

    key = f"notifications/{date}/{event["id"]}.json"
    sentence = f"{detail["severity"].upper()} incident #{detail["incidentId"]} '{detail["title"]}', SLA {detail["slaMinutes"]} min, due {detail["dueAt"][11:16]}Z"
    summary = {**detail, "eventId": event["id"], "message": sentence}

    return key, summary

def handler(event, context):
    key, summary = build_record(event)
    bucket = os.environ["NOTIFY_BUCKET"]
    s3.put_object(Bucket = bucket, Key=key, Body=json.dumps(summary), ContentType="application/json")
    log.info("notification_recorded", extra={"fields": {
        "eventId": event["id"],
        "incidentId": summary["incidentId"],
        "key": key,
    }})

if __name__ == "__main__":
    with open(sys.argv[1]) as f:
        sample = json.load(f)
    key, summary = build_record(sample)
    print(key)
    print(json.dumps(summary, indent=2))