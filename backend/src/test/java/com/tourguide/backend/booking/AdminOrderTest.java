package com.tourguide.backend.booking;

import com.tourguide.backend.AbstractIntegrationTest;
import com.tourguide.backend.security.JwtService;
import com.tourguide.backend.security.UserType;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin 订单管理 (MIN-50): filter / Excel export / 异常处理. */
@AutoConfigureMockMvc
class AdminOrderTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    AppUserRepository userRepo;
    @Autowired
    ScenicSessionRepository sessionRepo;
    @Autowired
    BookingOrderRepository orderRepo;

    private long seedOrder() {
        AppUser u = new AppUser();
        u.setNickname("订单游客");
        long uid = userRepo.save(u).getId();
        ScenicSession s = new ScenicSession();
        s.setTitle("订单管理场次");
        s.setType("PRIVATE");
        s.setSessionDate(LocalDate.now());
        s.setCapacity(5);
        s.setPriceFen(10000L);
        s.setStatus("OPEN");
        long sid = sessionRepo.save(s).getId();
        BookingOrder o = new BookingOrder();
        o.setOrderNo("ADM-" + sid);
        o.setUserId(uid);
        o.setSessionId(sid);
        o.setType("PRIVATE");
        o.setPeopleCount(2);
        o.setVisitDate(LocalDate.now());
        o.setAmountFen(20000L);
        o.setStatus("PAID");
        return orderRepo.save(o).getId();
    }

    @Test
    void filter_export_handle() throws Exception {
        long oid = seedOrder();
        String su = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_SUPER"));

        mvc.perform(get("/api/admin/orders").param("status", "PAID")
                        .header("Authorization", "Bearer " + su))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + oid + ")]").exists());

        byte[] xlsx = mvc.perform(get("/api/admin/orders/export")
                        .header("Authorization", "Bearer " + su))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(xlsx.length).isGreaterThan(0);
        assertThat(xlsx[0]).isEqualTo((byte) 'P'); // .xlsx is a zip ("PK")
        assertThat(xlsx[1]).isEqualTo((byte) 'K');

        mvc.perform(post("/api/admin/orders/" + oid + "/handle")
                        .header("Authorization", "Bearer " + su)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CANCEL\",\"reason\":\"游客投诉\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void export_requiresExportAuthority() throws Exception {
        String ops = jwt.issueAccessToken(1L, UserType.ADMIN, List.of("ADMIN_OPS")); // VIEW+OPERATE, no EXPORT
        mvc.perform(get("/api/admin/orders/export").header("Authorization", "Bearer " + ops))
                .andExpect(status().isForbidden());
    }
}
