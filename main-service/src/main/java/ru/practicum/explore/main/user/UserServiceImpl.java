package ru.practicum.explore.main.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.user.dto.NewUserRequest;
import ru.practicum.explore.main.user.dto.UserDto;
import ru.practicum.explore.main.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageRequest).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageRequest);
        }
        return users.stream().map(UserMapper::mapToUserDto).collect(Collectors.toList());
    }

    public UserDto addUser(NewUserRequest newUserRequest) {
        return UserMapper.mapToUserDto(userRepository.save(UserMapper.mapToUser(newUserRequest)));
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }
}
