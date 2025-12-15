package ewm.interaction.api.mappers;

import ewm.event.mapper.EventMapper;
import ewm.interaction.api.dto.UserDto;
import ewm.interaction.api.dto.UserShortDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface UserMapper {
    UserShortDto toUserShortDto(UserDto user);
}
