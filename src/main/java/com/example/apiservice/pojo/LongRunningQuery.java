package com.example.apiservice.pojo;

public record LongRunningQuery(
        long timeRunningMillis,
        String queryText
) {
}

