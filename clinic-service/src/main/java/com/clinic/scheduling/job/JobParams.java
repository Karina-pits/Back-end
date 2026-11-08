package com.clinic.scheduling.job;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

/**
 * Зчитування обов'язкових параметрів завдання.
 *
 * Обрана поведінка при помилці в ключі (друкарська помилка, ключ відсутній,
 * неправильний тип): ЯВНИЙ виняток JobExecutionException з поясненням,
 * а не мовчазна підстановка значення. Значення за замовчуванням уже лежать
 * у JobDetail, тому відсутність ключа означає помилку конфігурації.
 */
public final class JobParams {

    private JobParams() {
    }

    public static int requireInt(JobDataMap map, String key) throws JobExecutionException {
        requireKey(map, key);
        try {
            return map.getIntValue(key);
        } catch (RuntimeException e) {
            throw new JobExecutionException(
                    "Параметр завдання '" + key + "' має неправильний тип або значення: " + map.get(key), e);
        }
    }

    public static String requireString(JobDataMap map, String key) throws JobExecutionException {
        requireKey(map, key);
        Object value = map.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new JobExecutionException(
                    "Параметр завдання '" + key + "' має бути непорожнім рядком, а не: " + value);
        }
        return text;
    }

    private static void requireKey(JobDataMap map, String key) throws JobExecutionException {
        if (!map.containsKey(key)) {
            throw new JobExecutionException(
                    "Відсутній обов'язковий параметр завдання '" + key + "' (перевір написання ключа в JobDataMap)");
        }
    }
}
