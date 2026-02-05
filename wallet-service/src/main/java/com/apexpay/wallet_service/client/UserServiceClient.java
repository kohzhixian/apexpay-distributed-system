package com.apexpay.wallet_service.client;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.wallet_service.dto.ContactDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for interacting with the user service.
 * <p>
 * Provides methods for retrieving contact details needed for wallet transfers.
 * </p>
 */
@FeignClient(value = "userservice")
public interface UserServiceClient {

    /**
     * Retrieves contact details by recipient email for a given owner.
     * <p>
     * Used during wallet transfers to verify the recipient is in the sender's
     * contacts and to retrieve the recipient's wallet ID and user ID.
     * </p>
     *
     * @param ownerId        the user ID of the user initiating the transfer
     * @param recipientEmail the email of the recipient
     * @return contact details including userId and walletId
     */
    @GetMapping("/api/v1/contacts/recipient")
    ContactDetailsDto getContactByRecipientEmail(
            @RequestHeader(HttpHeaders.X_USER_ID) String ownerId,
            @RequestParam("recipientEmail") String recipientEmail
    );
}
