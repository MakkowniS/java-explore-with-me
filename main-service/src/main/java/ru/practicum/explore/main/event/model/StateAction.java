package ru.practicum.explore.main.event.model;

public enum StateAction {
    PUBLISH_EVENT, // Запрос админа на публикацию
    REJECT_EVENT, // Запрос админа на отклонение
    SEND_TO_REVIEW, // Запрос юзера на рассмотрение
    CANCEL_REVIEW // Запрос юзера на отмену
}
