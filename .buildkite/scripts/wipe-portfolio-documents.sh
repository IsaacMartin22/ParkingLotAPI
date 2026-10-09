#!/usr/bin/env sh
set -eu

if [ -z "${MONGODB_URI:-}" ]; then
  echo "MONGODB_URI is required."
  exit 1
fi

if [ -z "${MONGODB_DATABASE:-}" ]; then
  echo "MONGODB_DATABASE is required."
  exit 1
fi

if [ -z "${MONGODB_COLLECTION:-}" ]; then
  echo "MONGODB_COLLECTION is required."
  exit 1
fi

mongosh "$MONGODB_URI" --quiet --eval "
const result = db.getSiblingDB('$MONGODB_DATABASE')
  .getCollection('$MONGODB_COLLECTION')
  .deleteMany({});
print('Deleted documents: ' + result.deletedCount);
"
