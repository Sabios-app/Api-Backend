package br.com.sabios.api.notification.repository;

import br.com.sabios.api.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    List<DeviceToken> findByActiveTrue();

    List<DeviceToken> findByUserId(UUID userId);

    void deleteByFcmToken(String fcmToken);
}
