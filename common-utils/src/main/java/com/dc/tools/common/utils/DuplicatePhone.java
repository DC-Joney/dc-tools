package com.dc.tools.common.utils;

/**
 * 用于手机号去重校验
 * 最终大小占用内存 12M左右的空间，即可存储所有的手机号
 *
 * <p>
 * <p>
 * <h3>
 * 设计思想如下,将一个手机号拆分为三部分，分别是：
 * </h3>
 * <ul>
 *     <li>&nbsp;&nbsp;&nbsp;prefix: 前三位</li>
 *     <li>&nbsp;&nbsp;&nbsp;center：中间四位</li>
 *     <li>&nbsp;&nbsp;&nbsp;suffix：后边四位</li>
 *
 * </ul>
 *
 * <p>
 *     把上边拆分的三部分分别理解为三块，每一块都是一个单位，拆分为可以发现：
 *     <ol>
 *       <li>prefix最大存储是1-99，手机号默认是1xx开头，取出前三位-100则可以算出实际占用的数据</li>
 *       <li>center 最大存储是0-9999</li>
 *       <li>suffix 最大存储是0-9999</li>
 *     </ol>
 * </p>
 * <p>
 * 在拆分的基础上通过为每一段申请一个新的数组来保存需要存储的前缀数据，例如：13411896784，
 * 我们将这个手机号拆分为134、1189、6784 三部分，分别为prefix、center、suffix,
 * 每一个suffix可能对应很多个center，而每一个center则对应很多个prefix，为了保证其唯一性
 * 通过为每个单位的数字创建一个新的数组来存储前缀具体是否存在，以上面的手机号为例如下：<br/>
 * <p>
 *    <ol>
 *        <li>首先通过运算算出suffix对应的{@link DuplicatePhone#suffixBits}数组中的arrayIndex 与 bitIndex来判断是否存在对应的suffix</li>
 *        <li>在通过suffix从 {@link DuplicatePhone#centerBits} 数组中取出对应的二级数组</li>
 *        <li>在通过center从 {@link DuplicatePhone#prefixBits} 数组中取出对应的二级数组</li>
 *      </ol>
 * <p>
 * <br/>
 * <b>这样通过运算就可以确定其唯一性</b>
 *
 * <p>
 *     <br/>
 * 占用空间如下, ：
 *     <ol>
 *         <li>suffix: (10000 >> 5) / 1024 / 1024 = 1.23KB </li>
 *         <li>center: (10000 * (10000 >> 5)) * 4 / 1024 / 1024 = 11.9M </li>
 *         <li>prefix: (10000 * (100 >> 5)) * 4 / 1024 / 1024 = 0.11M</li>
 *
 *     </ol>
 * </p>
 *
 * @author zhangyang
 */
public class DuplicatePhone {

    /**
     * 用于存放prefix以及center指向prefix的数据
     */
    private int[][] prefixBits;

    /**
     * 用于存放suffix数据
     */
    private int[] suffixBits;

    /**
     * 用于存放center以及suffix指向center的数组
     */
    private int[][] centerBits;

    //手机号前三位占用的数据最多为100
    private static final int PREFIX_COUNT = 100;

    /**
     * 手机号后4位最多占用的数据
     */
    private static final int SUFFIX_COUNT = 10000;


    private static final int MASK = Integer.SIZE - 1;


    public DuplicatePhone() {
        init();
    }

    /**
     * 初始化数组
     */
    private void init() {
        int prefixArrSize = arrayIndex(PREFIX_COUNT);
        if (bitIndex(PREFIX_COUNT) != 0) {
            prefixArrSize += 1;
        }

        int suffixArrSize = arrayIndex(SUFFIX_COUNT);
        if (bitIndex(SUFFIX_COUNT) != 0) {
            suffixArrSize += 1;
        }
        this.suffixBits = new int[suffixArrSize];

        this.prefixBits = new int[SUFFIX_COUNT][prefixArrSize];
        this.centerBits = new int[SUFFIX_COUNT][suffixArrSize];
    }

    /**
     * 计算数据在数组中的下标位置
     */
    private static int arrayIndex(int number){
        return number >> 5;
    }

    /**
     * 计算数据在数组中的下标位置
     */
    private static int bitIndex(int number){
        return number & MASK;
    }


    public void addPhone(String phone) {
        int phonePrefix = getPhonePrefix(phone);
        int phoneCenter = getPhoneCenter(phone);
        int phoneSuffix = getPhoneSuffix(phone);

        //添加suffix bit位置
        int suffixArrIndex = arrayIndex(phoneSuffix);
        int suffixBitIndex = bitIndex(phoneSuffix);
        this.suffixBits[suffixArrIndex] |= (1 << suffixBitIndex);

        //添加center bit位置
        int[] centerBitArray = this.centerBits[phoneSuffix];
        int centerIndex = arrayIndex(phoneCenter);
        int centerBitIndex = bitIndex(phoneCenter);
        centerBitArray[centerIndex] |= (1 << centerBitIndex);

        //添加prefix bit位置
        int[] prefixBitArray = this.prefixBits[phoneCenter];
        int prefixArrIndex = arrayIndex(phonePrefix - 100);
        int prefixBitIndex = bitIndex(phonePrefix - 100);
        prefixBitArray[prefixArrIndex] |= (1 << prefixBitIndex);
    }


    public boolean isExists(String phone) {
        int phonePrefix = getPhonePrefix(phone);
        int phoneCenter = getPhoneCenter(phone);
        int phoneSuffix = getPhoneSuffix(phone);
        //获取suffix 对应的bit
        int suffixArrIndex = arrayIndex(phoneSuffix);
        int suffixBitIndex = bitIndex(phoneSuffix);
        boolean suffix = (this.suffixBits[suffixArrIndex] >> suffixBitIndex & 1) == 1;

        //如果suffix不存在，那么这个电话号就一定不存在
        if (!suffix)
            return false;

        //获取center对应的bit
        int[] centerBitArray = this.centerBits[phoneSuffix];
        int centerIndex = arrayIndex(phoneCenter);
        int centerBitIndex = bitIndex(phoneCenter);
        boolean center = (centerBitArray[centerIndex] >> centerBitIndex & 1) == 1;

        //获取suffix 对应的bit
        int[] prefixBitArray = this.prefixBits[phoneCenter];
        int prefixIndex = arrayIndex(phonePrefix - 100);
        int prefixBitIndex = bitIndex(phonePrefix - 100);
        boolean prefix = (prefixBitArray[prefixIndex] >> prefixBitIndex & 1) == 1;
        return center && prefix;
    }


    /**
     * 取出手机号前8位
     *
     * @param phone 手机号
     */
    private static int getPhonePrefix(String phone) {
        //取出前3位
        String prefix = phone.substring(0, 3);
        return Integer.parseInt(prefix);
    }

    /**
     * 取出手机号前8位
     *
     * @param phone 手机号
     */
    private static int getPhoneCenter(String phone) {
        //取出前3位
        String prefix = phone.substring(3, 7);
        return Integer.parseInt(prefix);
    }

    /**
     * 取出手机号后8位
     *
     * @param phone 手机号
     */
    private static int getPhoneSuffix(String phone) {
        String suffix = phone.substring(7);
        return Integer.parseInt(suffix);
    }


}
