package com.dengqn.igps2xingzhe.vo;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IGPSActivity implements Serializable {
    @Serial
    private static final long serialVersionUID = 2974104022106192054L;

    private String RideId;
    private String MemberId;
    private String Title;
    private String StartTime;
    private String StartTimeString;
    private String RideDistance;
    private String TotalAscent;
    private String MovingTime;
    private String OpenStatus;
    private String Status;
    private String SportType;

    public String getFileName() {
        return StrUtil.format("igpsport-{}-{}-{}.fit", RideId, StartTimeString, RideDistance);
    }
}
