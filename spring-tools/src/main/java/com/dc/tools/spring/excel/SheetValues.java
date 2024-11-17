package com.dc.tools.spring.excel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor(staticName = "create")
@Getter
@Setter
@ToString
public class SheetValues<T> {

    private List<T> dataList;

    private String sheetName;
}
