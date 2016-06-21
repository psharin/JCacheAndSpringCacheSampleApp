package net.nicoll.scratch.spring.cache;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stephane Nicoll
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractBookRepositoryTest {

	@Autowired
	protected BookRepository bookRepository;

	@Autowired
	protected CacheManager cacheManager;

	protected Cache defaultCache;

	@Before
	public void setUp() {
		this.defaultCache = cacheManager.getCache("default");
	}

	protected Object generateKey(Long id) {
		return id;
	}

	@Test
	public void get() {
		Object key = generateKey(0L);

		assertDefaultCacheMiss(key);
		Book book = bookRepository.findBook(0L);
		assertDefaultCacheHit(key, book);
		System.out.println("get()::: Found book " + book + " for key " + key);
	}

	@Test
	public void getWithCustomCacheResolver() {
		Cache anotherCache = cacheManager.getCache("another");
		Object key = generateKey(0L);

		assertCacheMiss(key, defaultCache, anotherCache);
		Book book = bookRepository.findBook(0L, "default");
		assertCacheHit(key, book, defaultCache);
		assertCacheMiss(key, anotherCache);
		
		System.out.println("getWithCustomCacheResolver()::: Found book " + book + " for key " + key);


		Object key2 = generateKey(1L);
		assertCacheMiss(key2, defaultCache, anotherCache);
		Book book2 = bookRepository.findBook(1L, "another");
		assertCacheHit(key2, book2, anotherCache);
		assertCacheMiss(key2, defaultCache);
		
		System.out.println("getWithCustomCacheResolver()::: Found book " + book2 + " for key " + key2);

	}


	@Test
	public void put() {
		Object key = generateKey(1L);

		Book book = bookRepository.findBook(1L); // initialize cache
		assertDefaultCacheHit(key, book);

		Book updatedBook = new Book(1L, "Another title");
		bookRepository.updateBook(1L, updatedBook);
		assertDefaultCacheHit(key, updatedBook);
		
		System.out.println("put()::: Updated book " + updatedBook + " for key " + key);

	}

	@Test
	public void evict() {
		Object key = generateKey(2L);

		Book book = bookRepository.findBook(2L); // initialize cache
		assertDefaultCacheHit(key, book);

		assertTrue(bookRepository.removeBook(2L));
		assertDefaultCacheMiss(key);
		
		System.out.println("evict()::: Removed book " + book + " for key " + key);

	}

	@Test
	public void evictAll() {
		bookRepository.findBook(3L);
		bookRepository.findBook(4L);
		
		System.out.println("evictAll()::: Found book " + bookRepository.findBook(3L) + " for key " + 3L);
		System.out.println("evictAll()::: Found book " + bookRepository.findBook(4L) + " for key " + 4L);


		assertFalse("Cache is not empty", isEmpty(defaultCache));
		bookRepository.removeAll();
		assertTrue("Cache should be empty", isEmpty(defaultCache));
		
		System.out.println("evictAll()::: Removed all ");

	}

	protected boolean isEmpty(Cache cache) { // assuming simple implementation
		return ((ConcurrentMapCache) cache).getNativeCache().isEmpty();
	}

	protected void assertCacheMiss(Object key, Cache... caches) {
		for (Cache cache : caches) {
			assertNull("No entry should have been found in " + cache + " with key " + key, cache.get(key));
		}
	}

	protected void assertCacheHit(Object key, Book book, Cache... caches) {
		for (Cache cache : caches) {
			Cache.ValueWrapper wrapper = cache.get(key);
			assertNotNull("An entry should have been found in " + cache + " with key " + key, wrapper);
			assertEquals("Wrong value for entry in " + cache + " with key " + key, book, wrapper.get());
		}
	}

	protected void assertDefaultCacheMiss(Object key) {
		assertCacheMiss(key, defaultCache);
	}

	protected void assertDefaultCacheHit(Object key, Book book) {
		assertCacheHit(key, book, defaultCache);
	}
}
