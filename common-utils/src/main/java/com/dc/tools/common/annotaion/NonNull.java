package com.dc.tools.common.annotaion;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import java.lang.annotation.*;

/**
 * A common Spring annotation to declare that annotated elements cannot be null.
 * Leverages JSR 305 meta-annotations to indicate nullability in Java to common tools
 * with JSR 305 support and used by Kotlin to infer nullability of Spring API.
 * <p></p>
 * Should be used at parameter, return value, and field level.
 * Method overrides should repeat parent @NonNull annotations unless they behave differently.
 *
 * Fork from spring
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierNickname
public @interface NonNull {
}
