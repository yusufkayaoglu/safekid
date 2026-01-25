package com.safekid.parent;

import com.safekid.parent.dto.ChildCreateRequest;
import com.safekid.parent.dto.ChildResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parent")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @PostMapping("/children")
    public ResponseEntity<ChildResponse> addChild(@RequestBody ChildCreateRequest req) {
        return ResponseEntity.ok(parentService.addChild(req));
    }
}
