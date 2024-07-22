package com.dengqn.igps2xingzhe.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IGPSActivityResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -2545669204414828896L;

    private Integer total;

    private List<IGPSActivity> item;

    private Integer unit;
}
