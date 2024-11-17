package com.dc.tools.spring.transaction;

import lombok.experimental.UtilityClass;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * 编程式事务对象工厂
 *
 * <pre>
 *
 *     事务定义信息
 *     TransactionDefinition definition = new  DefaultTransactionDefinition();
 *
 *     事务状态信息
 *     TransactionStatus status = transactionManager.getTransaction(definition);
 *
 *     try{
 *
 *        do something
 *     }catch(Exception e){
 *
 *         if(!status.isCompleted()){
 *             e.setRollbackOnly(true);
 *             e.rollback(status);
 *         }
 *
 *     }
 *
 * </pre>
 *
 * @see PlatformTransactionManager 事务管理器
 * @see org.springframework.transaction.TransactionStatus 事务状态信息
 * @see org.springframework.transaction.support.DefaultTransactionStatus  事务状态信息-默认实现（包装器）
 * @see TransactionTemplate 编程式事务对象，封装了包括回滚 提交等行为
 * @see TransactionDefinition 事务信息(传播行为、隔离机制、超时等等)
 * @see org.springframework.transaction.support.DefaultTransactionDefinition 事务信息的默认实现
 * @see org.springframework.transaction.support.TransactionSynchronizationManager 事务与线程关联挂载器
 *
 * @author zhangyang
 * @date 2020-07-23
 */
@UtilityClass
public class TransactionTemplateFactory {


    /**
     * 创建编程式事务对象
     */
    public TransactionTemplate createTemplate(PlatformTransactionManager transactionManager, String transactionName) {
        return createTemplate(transactionManager, transactionName, -1, null, TransactionDefinition.PROPAGATION_REQUIRED);
    }


    /**
     * 创建编程式事务对象
     */
    public TransactionTemplate createTemplate(PlatformTransactionManager transactionManager, String transactionName, long timeout, TimeUnit timeUnit) {
        Assert.isTrue(timeout >= 0, "The template timeout must be gt 0");
        Assert.notNull(timeUnit, "The template timeout of unit must not be null");
        return createTemplate(transactionManager, transactionName, timeout, timeUnit, TransactionDefinition.PROPAGATION_REQUIRED);
    }


    /**
     * 创建编程式事务对象
     */
    public TransactionTemplate createTemplate(PlatformTransactionManager transactionManager, String transactionName, long timeout, TimeUnit timeUnit, int propagationBehavior) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.setName(transactionName);

        if (timeout >=0 && timeUnit != null){
            transactionTemplate.setTimeout((int) timeUnit.toSeconds(timeout));
        }
        return transactionTemplate;
    }


    /**
     * 创建定制的编程式事务对象
     *
     * @param transactionManager 事务管理器
     * @return 创建以当前线程命名的事务对象
     */
    public TransactionTemplate createThreadTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.setName(Thread.currentThread().getName());
        return transactionTemplate;
    }

}
