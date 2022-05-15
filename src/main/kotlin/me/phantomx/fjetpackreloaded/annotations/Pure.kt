package me.phantomx.fjetpackreloaded.annotations

/**
 * Don't modify value, like put 'field' or "field"
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Pure
