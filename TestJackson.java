import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
public class TestJackson {
    @Data
    public static class Dto {
        private Boolean isActive;
    }
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Dto dto = mapper.readValue("{\"isActive\": true}", Dto.class);
        System.out.println("isActive mapped? " + dto.getIsActive());
        
        Dto dto2 = mapper.readValue("{\"active\": true}", Dto.class);
        System.out.println("active mapped? " + dto2.getIsActive());
    }
}
