package com.config;

import com.entity.ProductCategory;
import com.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductCategoryInitializer implements ApplicationRunner {

    private static final String FRUIT_SOURCE =
            "Bảng 2, Tạp chí Khoa học Trường Đại học Cần Thơ (2021); nguồn bảng: Kader (2002)";

    private static final String VEGETABLE_SOURCE =
            "FAO (2003), Handling and Preservation of Fruits and Vegetables "
                    + "by Combined Methods for Rural Areas, Bảng 5.1";

    private static final String PITAYA_SOURCE =
            "FAO, Manual for the Preparation and Sale of Fruits and Vegetables, Bảng 5";

    private final ProductCategoryRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> existingNames = repository.findAll().stream()
                .map(item -> item.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());


        seed(existingNames, "Sơ-ri", 0.0, 0.0, 85.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Chuối", 13.0, 15.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Sa-kê", 13.0, 15.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Khế", 9.0, 10.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Mãng cầu ta", 13.0, 13.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Dừa", 0.0, 2.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Chà là", -18.0, 0.0, 75.0, 75.0, FRUIT_SOURCE);
        seed(existingNames, "Sầu riêng", 4.0, 6.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Nho", 0.0, 5.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Ổi", 5.0, 10.0, 90.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Mít", 13.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Bòn bon", 11.0, 14.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Nhãn", 4.0, 7.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Mamey sapote", 13.0, 15.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Xoài", 13.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Măng cụt", 13.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Đu đủ", 7.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Khóm", 7.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Chuối mễ", 13.0, 15.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Lựu", 5.0, 7.2, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Chôm chôm", 12.0, 12.0, 90.0, 95.0, FRUIT_SOURCE);
        seed(existingNames, "Hồng xiêm", 15.0, 20.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Mãng cầu xiêm", 13.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);
        seed(existingNames, "Cóc", 13.0, 13.0, 85.0, 90.0, FRUIT_SOURCE);


        seed(existingNames, "Thanh long", 6.0, 8.0, 85.0, 95.0, PITAYA_SOURCE);

        // Rau củ: Bảng 5.1 của FAO
        seed(existingNames, "Bông cải xanh", 0.0, 0.0, 90.0, 95.0, VEGETABLE_SOURCE);
        seed(existingNames, "Hành tây", 1.0, 2.0, 70.0, 75.0, VEGETABLE_SOURCE);
        seed(existingNames, "Tỏi", 0.0, 0.0, 70.0, 75.0, VEGETABLE_SOURCE);
        seed(existingNames, "Cà tím", 10.0, 12.0, 95.0, 95.0, VEGETABLE_SOURCE);
        seed(existingNames, "Dưa leo", 10.0, 13.0, 95.0, 95.0, VEGETABLE_SOURCE);
    }

    private void seed(
            Set<String> existingNames,
            String name,
            double temperatureMin,
            double temperatureMax,
            double humidityMin,
            double humidityMax,
            String source
    ) {
        if (!existingNames.add(name.toLowerCase(Locale.ROOT))) {
            return;
        }

        repository.save(ProductCategory.builder()
                .name(name)
                .temperatureMin(temperatureMin)
                .temperatureMax(temperatureMax)
                .humidityMin(humidityMin)
                .humidityMax(humidityMax)
                .description(source)
                .active(true)
                .build());
    }
}