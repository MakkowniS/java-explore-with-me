package ru.practicum.explore.main.user;

import ru.practicum.explore.main.user.dto.NewUserRequest;
import ru.practicum.explore.main.user.dto.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(List<Long> ids, int from, int size);
    UserDto addUser(NewUserRequest newUserRequest);
    void deleteUser(Long userId);

}
