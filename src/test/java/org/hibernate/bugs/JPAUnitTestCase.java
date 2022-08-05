package org.hibernate.bugs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.*;


/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
public class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
	}

	@After
	public void destroy() {
		entityManagerFactory.close();
	}

	@Test
	public void fullEntityGraph() throws Exception {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		final Person jens = createFriendsAndClear(entityManager);


		final TypedQuery<Person> query = createQuery(entityManager, jens);
		query.setHint("javax.persistence.fetchgraph", createEntityGraph(entityManager));
		final List<Person> result = query.getResultList(); //  <--- This fails when executed for the second time.

		assertSoftly(softly -> {

			for (Person person : result) {
				System.out.println("=========================== Jens ");

				softly.assertThat(Persistence.getPersistenceUtil().isLoaded(person.getHobbies())).describedAs("jens hobbies are loaded").isTrue();
				softly.assertThat(Persistence.getPersistenceUtil().isLoaded(person.getFriends())).describedAs("jens friends are loaded").isTrue();

				for (Person friend : jens.getFriends()) {

					System.out.println("=========================== Leif or Frank");
					softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friend.getHobbies())).describedAs("leifs hobbies are loaded").isTrue();
					softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friend.getFriends())).describedAs("leifs friends are loaded").isTrue();

					for (Person friendsFriend : friend.getFriends()) {
						System.out.println("=========================== Frank ");
						softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friendsFriend.getHobbies())).describedAs("frank hobbies are loaded").isTrue();
//						softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friendsFriend.friends)).describedAs("franks friends are loaded").isTrue();
					}
				}
			}
		});


		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void noEntityGraph() {

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		final Person jens = createFriendsAndClear(entityManager);


		final TypedQuery<Person> query = createQuery(entityManager, jens);
		final List<Person> result = query.getResultList(); //  <--- This fails when executed for the second time.

		assertSoftly(softly -> {

			for (Person person : result) {
				System.out.println("=========================== Jens ");

				softly.assertThat(Persistence.getPersistenceUtil().isLoaded(person.getHobbies())).describedAs("jens hobbies are loaded").isFalse();
				softly.assertThat(Persistence.getPersistenceUtil().isLoaded(person.getFriends())).describedAs("jens friends are loaded").isFalse();

				for (Person friend : jens.getFriends()) {
					// how do we even get here when the log shows only a single select statement ???
					System.out.println("=========================== Leif or Frank");
					softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friend.getHobbies())).describedAs("leifs hobbies are loaded").isTrue(); // why are these and the following loaded?
					softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friend.getFriends())).describedAs("leifs friends are loaded").isTrue();

					for (Person friendsFriend : friend.getFriends()) {
						System.out.println("=========================== Frank ");
						softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friendsFriend.getHobbies())).describedAs("frank hobbies are loaded").isTrue();
//						softly.assertThat(Persistence.getPersistenceUtil().isLoaded(friendsFriend.friends)).describedAs("franks friends are loaded").isTrue();
					}
				}
			}
		});


		entityManager.getTransaction().commit();
		entityManager.close();
	}

	private TypedQuery<Person> createQuery(EntityManager entityManager, Person jens) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Person> personQuery = cb.createQuery(Person.class);
		final Root<Person> root = personQuery.from(Person.class);
		personQuery.where(cb.equal(root.get("id"), jens.id));

		final TypedQuery<Person> query = entityManager.createQuery(personQuery);
		return query;
	}

	private EntityGraph<Person> createEntityGraph(EntityManager entityManager) {
		final EntityGraph<Person> eg = entityManager.createEntityGraph(Person.class);
		eg.addAttributeNodes("hobbies");
		final Subgraph<Object> friends = eg.addSubgraph("friends");
		friends.addAttributeNodes("hobbies");
		final Subgraph<Object> friendsFriends = eg.addSubgraph("friends");
		friends.addAttributeNodes("hobbies");
		return eg;
	}

	private Person createFriendsAndClear(EntityManager entityManager) {
		final Hobby juggling = new Hobby("juggling");
		final Hobby music = new Hobby("playing music");
		final Hobby gaming = new Hobby("board gaming");
		final Hobby climbing = new Hobby("climbing");

		final Person frank = new Person("Frank");
		frank.getHobbies().add(juggling);
		frank.getHobbies().add(music);
		entityManager.persist(frank);


		final Person leif = new Person("Leif");
		leif.getFriends().add(frank);

		leif.getHobbies().add(gaming);
		leif.getHobbies().add(music);
		entityManager.persist(leif);


		final Person jens = new Person("Jens");
		jens.getFriends().add(leif);
		jens.getFriends().add(frank);

		jens.getHobbies().add(climbing);
		jens.getHobbies().add(gaming);
		entityManager.persist(jens);

		entityManager.flush();
		entityManager.clear();
		return jens;
	}
}
