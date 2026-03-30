package ru.practicum.explore.main.user;

import ru.practicum.explore.main.user.dto.NewUserRequest;
import ru.practicum.explore.main.user.dto.UserDto;
import ru.practicum.explore.main.user.model.User;

public class UserMapper {

    public static User mapToUser(NewUserRequest newUserRequest){
        return User.builder()
                .name(newUserRequest.getName())
                .email(newUserRequest.getEmail())
                .build();
    }

    public static UserDto mapToUserDto(User user){
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
