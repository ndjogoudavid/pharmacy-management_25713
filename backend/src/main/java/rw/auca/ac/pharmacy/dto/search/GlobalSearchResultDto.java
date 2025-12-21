package rw.auca.ac.pharmacy.dto.search;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GlobalSearchResultDto {
    private String type;
    private Long id;
    private String label;
    private String description;
    private String subtitle;
    private String extra;
    private String url;
}
