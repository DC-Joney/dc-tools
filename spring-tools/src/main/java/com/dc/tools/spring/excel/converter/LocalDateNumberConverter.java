package com.dc.tools.spring.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * java8时间转换 Number -> LocalDate
 *
 * @author zhangyang
 */
public class LocalDateNumberConverter implements Converter<LocalDate> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.NUMBER;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {

        if (contentProperty == null || contentProperty.getDateTimeFormatProperty() == null) {
            Date javaDate = DateUtil.getJavaDate(cellData.getNumberValue().doubleValue(), globalConfiguration.getUse1904windowing(), null);
            return javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        } else {
            Date javaDate = DateUtil.getJavaDate(cellData.getNumberValue().doubleValue(),
                    contentProperty.getDateTimeFormatProperty().getUse1904windowing(), null);
            return javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    @Override
    public WriteCellData<?> convertToExcelData(LocalDate localDate, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {

        Date value = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        if (contentProperty == null || contentProperty.getDateTimeFormatProperty() == null) {
            return new WriteCellData<>(
                    BigDecimal.valueOf(DateUtil.getExcelDate(value, globalConfiguration.getUse1904windowing())));
        } else {
            return new WriteCellData<>(BigDecimal.valueOf(
                    DateUtil.getExcelDate(value, contentProperty.getDateTimeFormatProperty().getUse1904windowing())));
        }
    }


}
