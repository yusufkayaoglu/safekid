package com.safekid.parent.controller;

import com.safekid.parent.dto.ChildCreateRequest;
import com.safekid.parent.dto.ChildResponse;
import com.safekid.parent.service.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ChildService childService;

    // POST /parent/children
    @PostMapping("/children")
    public ResponseEntity<ChildResponse> addChild(@RequestBody ChildCreateRequest req) {
        return ResponseEntity.ok(childService.addChild(req));
    }

    // GET /parent/children
    @GetMapping("/children")
    public ResponseEntity<List<ChildResponse>> listChildren() {
        return ResponseEntity.ok(childService.listMyChildren());
    }

    // GET /parent/children/{childId}
    @GetMapping("/children/{childId}")
    public ResponseEntity<ChildResponse> getChild(@PathVariable String childId) {
        return ResponseEntity.ok(childService.getChild(childId));
    }

    // DELETE /parent/children/{childId}
    @DeleteMapping("/children/{childId}")
    public ResponseEntity<Void> deleteChild(@PathVariable String childId) {
        childService.deleteChild(childId);
        return ResponseEntity.ok().build();
    }
}
