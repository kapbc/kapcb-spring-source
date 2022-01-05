/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.redis;

import java.util.Properties;

import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.ReactiveRedisClusterConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RedisReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Nikolay Rybak
 * @author Artsiom Yudovin
 * @author Scott Frederick
 */
class RedisReactiveHealthIndicatorTests {

	@Test
	void redisIsUp() {
		Properties info = new Properties();
		info.put("redis_version", "2.8.9");
		ReactiveRedisConnection redisConnection = mock(ReactiveRedisConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		ReactiveServerCommands commands = mock(ReactiveServerCommands.class);
		given(commands.info("server")).willReturn(Mono.just(info));
		RedisReactiveHealthIndicator healthIndicator = createHealthIndicator(redisConnection, commands);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("version");
			assertThat(h.getDetails().get("version")).isEqualTo("2.8.9");
		}).verifyComplete();
		verify(redisConnection).closeLater();
	}

	@Test
	void healthWhenClusterStateIsAbsentShouldBeUp() {
		ReactiveRedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory(null);
		RedisReactiveHealthIndicator healthIndicator = new RedisReactiveHealthIndicator(redisConnectionFactory);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails().get("cluster_size")).isEqualTo(4L);
			assertThat(h.getDetails().get("slots_up")).isEqualTo(4L);
			assertThat(h.getDetails().get("slots_fail")).isEqualTo(0L);
		}).verifyComplete();
		verify(redisConnectionFactory.getReactiveConnection()).closeLater();
	}

	@Test
	void healthWhenClusterStateIsOkShouldBeUp() {
		ReactiveRedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory("ok");
		RedisReactiveHealthIndicator healthIndicator = new RedisReactiveHealthIndicator(redisConnectionFactory);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails().get("cluster_size")).isEqualTo(4L);
			assertThat(h.getDetails().get("slots_up")).isEqualTo(4L);
			assertThat(h.getDetails().get("slots_fail")).isEqualTo(0L);
		}).verifyComplete();
	}

	@Test
	void healthWhenClusterStateIsFailShouldBeDown() {
		ReactiveRedisConnectionFactory redisConnectionFactory = createClusterConnectionFactory("fail");
		RedisReactiveHealthIndicator healthIndicator = new RedisReactiveHealthIndicator(redisConnectionFactory);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.DOWN);
			assertThat(h.getDetails().get("slots_up")).isEqualTo(3L);
			assertThat(h.getDetails().get("slots_fail")).isEqualTo(1L);
		}).verifyComplete();
	}

	@Test
	void redisCommandIsDown() {
		ReactiveServerCommands commands = mock(ReactiveServerCommands.class);
		given(commands.info("server")).willReturn(Mono.error(new RedisConnectionFailureException("Connection failed")));
		ReactiveRedisConnection redisConnection = mock(ReactiveRedisConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		RedisReactiveHealthIndicator healthIndicator = createHealthIndicator(redisConnection, commands);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> assertThat(h.getStatus()).isEqualTo(Status.DOWN))
				.verifyComplete();
		verify(redisConnection).closeLater();
	}

	@Test
	void redisConnectionIsDown() {
		ReactiveRedisConnectionFactory redisConnectionFactory = mock(ReactiveRedisConnectionFactory.class);
		given(redisConnectionFactory.getReactiveConnection())
				.willThrow(new RedisConnectionException("Unable to connect to localhost:6379"));
		RedisReactiveHealthIndicator healthIndicator = new RedisReactiveHealthIndicator(redisConnectionFactory);
		Mono<Health> health = healthIndicator.health();
		StepVerifier.create(health).consumeNextWith((h) -> assertThat(h.getStatus()).isEqualTo(Status.DOWN))
				.verifyComplete();
	}

	private RedisReactiveHealthIndicator createHealthIndicator(ReactiveRedisConnection redisConnection,
			ReactiveServerCommands serverCommands) {
		ReactiveRedisConnectionFactory redisConnectionFactory = mock(ReactiveRedisConnectionFactory.class);
		given(redisConnectionFactory.getReactiveConnection()).willReturn(redisConnection);
		given(redisConnection.serverCommands()).willReturn(serverCommands);
		return new RedisReactiveHealthIndicator(redisConnectionFactory);
	}

	private ReactiveRedisConnectionFactory createClusterConnectionFactory(String state) {
		Properties clusterProperties = new Properties();
		if (state != null) {
			clusterProperties.setProperty("cluster_state", state);
		}
		clusterProperties.setProperty("cluster_size", "4");
		boolean failure = "fail".equals(state);
		clusterProperties.setProperty("cluster_slots_ok", failure ? "3" : "4");
		clusterProperties.setProperty("cluster_slots_fail", failure ? "1" : "0");
		ReactiveRedisClusterConnection redisConnection = mock(ReactiveRedisClusterConnection.class);
		given(redisConnection.closeLater()).willReturn(Mono.empty());
		given(redisConnection.clusterGetClusterInfo()).willReturn(Mono.just(new ClusterInfo(clusterProperties)));
		ReactiveRedisConnectionFactory redisConnectionFactory = mock(ReactiveRedisConnectionFactory.class);
		given(redisConnectionFactory.getReactiveConnection()).willReturn(redisConnection);
		return redisConnectionFactory;
	}

}
