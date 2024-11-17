package com.dc.tools.spring.excel.rule.write;//package com.turing.pt.common.excel.rule.write;
//
//
//import com.alibaba.excel.metadata.Cell;
//import com.alibaba.excel.metadata.Head;
//import com.alibaba.excel.metadata.data.WriteCellData;
//import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
//import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
//import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
//import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
//import com.alibaba.excel.write.style.AbstractCellStyleStrategy;
//import com.google.common.base.Objects;
//import com.google.common.collect.ImmutableMap;
//import com.turing.pt.common.excel.rule.ExcelWriteRule;
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import org.apache.poi.ss.usermodel.CellType;
//import org.springframework.core.ResolvableType;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * @param <T>
// * @author zhangyang
// */
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE)
//public abstract class AbstractExcelWriteRule<T> extends AbstractCellStyleStrategy
//        implements ExcelWriteRule<T> {
//
//    private static final String FORMULA_ATTRIBUTE = "=Formula=>";
//
//    /**
//     * 代表开始行号的占位符
//     */
//    private static final String FORMULA_START_ATTRIBUTE = "{start}";
//
//    /**
//     * 代表结束行号的占位符
//     */
//    private static final String FORMULA_END_ATTRIBUTE = "{end}";
//
//    /**
//     * 填充的 sheet 页面
//     */
//    @Getter
//    final int sheetNo;
//
//    @Override
//    public int sheetNo() {
//        return sheetNo;
//    }
//
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public Class<T> headerClass() {
//        return (Class<T>) ResolvableType.forInstance(this).resolveGeneric(0);
//    }
//
//    @Override
//    public Map<String, Object> getHeaderMap() {
//
//        //将公式 与 顶部其他数据放入headerMap中
//        ImmutableMap.Builder<String, Object> excelMap = ImmutableMap.builder();
//
//        Map<String, Object> headerMap = generateHeaderMap();
//        Map<String, String> formulaMap = generateFormulaMap();
//
//        if (!CollectionUtils.isEmpty(headerMap)) {
//            excelMap.putAll(headerMap);
//        }
//
//        List<T> dataList = fillDataList();
//
//        //做公式计算
//        if (!CollectionUtils.isEmpty(formulaMap)) {
//
//            formulaMap.forEach((text, formula) -> {
//                String start = StringUtils.replace(String.valueOf(formula), FORMULA_START_ATTRIBUTE, String.valueOf(getStartNum() + 1));
//
//                //数据是否为空做计算
//                int endNum = dataList.size() > 0 ? getStartNum() + dataList.size() : getStartNum() + 1;
//
//                //替换公式
//                String replaceFormula = StringUtils.replace(start, FORMULA_END_ATTRIBUTE, String.valueOf(endNum));
//
//                //将公式放入hashMap
//                excelMap.put(text, replaceFormula);
//            });
//        }
//
//        return excelMap.build();
//    }
//
//
//    @Override
//    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
//        writeSheetHolder.getCachedSheet().setForceFormulaRecalculation(true);
//    }
//
//    @Override
//    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
//        if (Objects.equal(cell.getCellTypeEnum(), CellType.STRING)) {
//            //将特定类型转为公式
//            if (cell.getStringCellValue().startsWith(FORMULA_ATTRIBUTE)) {
//                cell.setCellType(CellType.FORMULA);
//                cell.setCellFormula(cellDataList.get(0).toString().replace(FORMULA_ATTRIBUTE, ""));
//            }
//        }
//    }
//
//    @Override
//    public void beforeCellCreate(CellWriteHandlerContext context) {
//        super.beforeCellCreate(context);
//    }
//
//    /**
//     * 初始化excel页面的Cell 样式
//     *
//     * @param workbook 代表 一个excel文件
//     */
//    @Override
//    protected void initCellStyle(Workboo workbook) {
//
//    }
//
//    /**
//     * 设置 head 的excel样式
//     */
//    @Override
//    protected void setHeadCellStyle(Cell cell, Head head, Integer relativeRowIndex) {
//
//    }
//
//    /**
//     * 设置 excel 的内容格式央视
//     */
//    @Override
//    protected void setContentCellStyle(Cell cell, Head head, Integer relativeRowIndex) {
//
//    }
//
//
//    /**
//     * 需要填充的信息，比如 时间等
//     *
//     * @apiNote 填充都是非java bean class 类型数据
//     */
//    protected abstract Map<String, Object> generateHeaderMap();
//
//    /**
//     * 生成的公式map
//     * <p>
//     * 规则： {start} 开始的行数
//     * <p>
//     * {end} 结束的行数
//     * <p>
//     * 公式: 以 =Formula=> 开头，代表是计算的公式
//     */
//    protected abstract Map<String, String> generateFormulaMap();
//
//
//    /**
//     * 开始写入的行数
//     */
//    protected abstract int getStartNum();
//
//
//}
