package com.entrepidea.java.objectpool;

/**
 * An interface defining life-cycle methods for
 * instances to be used in an
 * {@link ObjectPool ObjectPool}.
 * <p>
 * By contract, when an {@link ObjectPool ObjectPool}
 * delegates to a <tt>PoolableObjectFactory</tt>,
 * <ol>
 *  <li>
 *   {@link #makeObject makeObject} 
 *   is called  whenever a new instance is needed.
 *  </li>
 *  <li>
 *   {@link #activateObject activateObject} 
 *   is invoked on every instance before it is returned from the
 *   pool.
 *  </li>
 *  <li>
 *   {@link #passivateObject passivateObject} 
 *   is invoked on every instance when it is returned to the
 *   pool.
 *  </li>
 *  <li>
 *   {@link #destroyObject destroyObject} 
 *   is invoked on every instance when it is being "dropped" from the
 *   pool (whether due to the response from
 *   {@link #validateObject validateObject}, or
 *   for reasons specific to the pool implementation.)
 *  </li>
 *  <li>
 *   {@link #validateObject validateObject} 
 *   is invoked in an implementation-specific fashion to determine if an instance
 *   is still valid to be returned by the pool.
 *   It will only be invoked on an {@link #activateObject "activated"}
 *   instance.
 *  </li>
 * </ol>
 *
 * @see ObjectPool
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.1 $ $Date: 2013-11-07 05:04:46 $ 
 */
public interface PoolableObjectFactory<T> {
  /**
   * Creates an instance that can be returned by the pool.
   * @return an instance that can be returned by the pool.
   */
  T makeObject() throws RuntimeException;

  /**
   * Destroys an instance no longer needed by the pool.
   * @param obj the instance to be destroyed
   */
  void destroyObject(T obj) throws RuntimeException;

  /**
   * Ensures that the instance is safe to be returned by the pool.
   * Returns <tt>false</tt> if this object should be destroyed.
   * @param obj the instance to be validated
   * @return <tt>false</tt> if this <i>obj</i> is not valid and should
   *         be dropped from the pool, <tt>true</tt> otherwise.
   */
  boolean validateObject(T obj);

  /**
   * Reinitialize an instance to be returned by the pool.
   * @param obj the instance to be activated
   */
  void activateObject(T obj) throws RuntimeException;

  /**
   * Uninitialize an instance to be returned to the pool.
   * @param obj the instance to be passivated
   */
  void passivateObject(T obj) throws RuntimeException;
}

